package ru.ifmo.backend_2021.connections

import cask.endpoints.WsHandler
import cask.util.Ws.Event
import cask.util.{Logger, Ws}
import cask.{WsActor, WsChannelActor}
import ru.ifmo.backend_2021.RedditApplication.{messageFilter, messageList}

object WsConnectionPool {
  def apply(): ConnectionPool = new ConnectionPoolImpl()
}

class ConnectionPoolImpl extends ConnectionPool {
  private var openConnections: Set[WsChannelActor] = Set.empty[WsChannelActor]
  private var channelFilters: Map[WsChannelActor, String] =
    Map.empty[WsChannelActor, String]
  def getConnections: List[WsChannelActor] =
    synchronized(openConnections.toList)
  def send(event: Event): WsChannelActor => Unit = _.send(event)
  def sendAll(event: WsChannelActor => Event): Unit = for (
    conn <- synchronized(openConnections)
  )
    send(event(conn))(conn)
  def addConnection(
      connection: WsChannelActor
  )(implicit ac: castor.Context, log: Logger): WsActor = {
    synchronized {
      openConnections += connection
    }
    WsActor { case Ws.Close(_, _) =>
      synchronized {
        channelFilters -= connection
        openConnections -= connection
      }
    }
  }
  def wsHandler(onConnect: WsChannelActor => Unit)(implicit
      ac: castor.Context,
      log: Logger
  ): WsHandler = WsHandler { connection =>
    log.debug("New Connection")
    onConnect(connection)
    addConnection(connection)
    WsActor {
      case cask.Ws.Text(data) => {
        if (data.contains("filter?=")) {
          val username = data.replace("filter?=", "")
          channelFilters += connection -> username
          connection.send(cask.Ws.Text(messageFilter(username).render))
        } else {
          connection.send(cask.Ws.Text(messageList().render))
        }
      }
    }
  }
  def getChannelFilter(channel: WsChannelActor): Option[String] = {
    channelFilters.get(channel)
  }
}
