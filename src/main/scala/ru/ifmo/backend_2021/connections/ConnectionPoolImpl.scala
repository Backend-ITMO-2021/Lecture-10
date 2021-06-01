package ru.ifmo.backend_2021.connections

import cask.endpoints.WsHandler
import cask.util.Ws.Event
import cask.util.{Logger, Ws}
import cask.{WsActor, WsChannelActor, WsClient}
import ru.ifmo.backend_2021.{AppUser, AppUserDTO}
import ru.ifmo.backend_2021.RedditApplication.{connectionPool, db, messageList, userFilter}
import scalatags.Text.all.s

object WsConnectionPool {
  def apply(): ConnectionPool = new ConnectionPoolImpl()
}

class ConnectionPoolImpl extends ConnectionPool {
  private var openConnections: Set[WsChannelActor] = Set.empty[WsChannelActor]
  private var openConnectionsUser = Map.empty[WsChannelActor, AppUser]

  override def getConnectionUser(channelActor: WsChannelActor): Option[AppUser] = openConnectionsUser.get(channelActor)

  def getOrCreateAppUser(nickName: String): AppUser = db.getUserByNickname(nickName) match {
    case Some(userObj) => userObj
    case None =>
      println("Creation user")
      db.addUser(AppUserDTO(nickName))
      db.getUserByNickname(nickName) match {
        case Some(value) => value
        case None => AppUser(1, "errorUser", None, isCascade = false)
      }
  }

  def getConnections: List[WsChannelActor] =
    synchronized(openConnections.toList)
  def send(event: Event): WsChannelActor => Unit = _.send(event)
  def sendAll(eventActor: WsChannelActor => Event): Unit = for (conn <- synchronized(openConnections)) send(eventActor(conn))(conn)
  def addConnection(connection: WsChannelActor)(nickName: String)(implicit ac: castor.Context, log: Logger) = {
    println("WS: new user")
    synchronized {
      openConnections += connection
      val user = getOrCreateAppUser(nickName)
      openConnectionsUser += connection -> user
      if (user.userFilter.isDefined) connection.send(cask.Ws.Text(s"filter#${user.userFilter.getOrElse("")}"))
    }
    connection.send(cask.Ws.Text(messageList(userFilter(None)).render))
  }
  def wsHandler(onConnect: WsChannelActor => Unit)(nickName: String)(implicit ac: castor.Context, log: Logger): WsHandler = WsHandler { connection =>
    log.debug("New Connection")
    log.debug(s"Nickname: ${nickName}")
    onConnect(connection)
    addConnection(connection)(nickName)

    WsActor {
      case cask.Ws.Text(data) =>
        println(data)
        val prevConnUser = getConnectionUser(connection) match {
          case Some(value) => value
          case None => getOrCreateAppUser(nickName)
        }
        if (data.contains("filter=")) {
          if (data.replace("filter=", "").nonEmpty) {
            openConnectionsUser += connection -> prevConnUser.copy(userFilter = Some(data.replace("filter=", "")))
          } else {
            openConnectionsUser += connection -> prevConnUser.copy(userFilter = None)
          }
        }

        if (data.contains("changeDisplay?")) {
          openConnectionsUser += connection -> prevConnUser.copy(isCascade = !prevConnUser.isCascade)
          connection.send(cask.Ws.Text(s"display#${!prevConnUser.isCascade}"))
        }

        val connUserParams = getConnectionUser(connection).getOrElse(AppUser(1, "errorUser", None, isCascade = false))
        connection.send(cask.Ws.Text(messageList(userFilter(connUserParams.userFilter), connUserParams.isCascade).render))

      case Ws.Close(_, _) => synchronized {
        openConnectionsUser.get(connection) match {
          case Some(user) => db.updateUserFilterById(user.id, user.userFilter)
          case None => println("Some error")
        }
        openConnectionsUser -= connection
        openConnections -= connection
      }
    }
  }
}
