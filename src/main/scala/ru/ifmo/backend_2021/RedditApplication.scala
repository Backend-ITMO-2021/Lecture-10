package ru.ifmo.backend_2021

import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.db.{DBAdapter, MessageDB}
import scalatags.Text.all._
import scalatags.generic
import scalatags.text.Builder

object RedditApplication extends cask.MainRoutes {
  val serverUrl = s"http://$host:$port"
  println(port)
  val db: MessageDB = new DBAdapter()
  val connectionPool: ConnectionPool = WsConnectionPool()

  @cask.staticResources("/static")
  def staticResourceRoutes() = "static"

  @cask.get("/")
  def hello(): Document = doctype("html")(
    html(
      head(
        link(rel := "stylesheet", href := ApplicationUtils.styles),
        link(rel := "stylesheet", href := ApplicationUtils.styles2),
        script(src := "/static/app.js")
      ),
      body(
        div(cls := "container")(
          h1("Reddit: Swain is mad :("),
          div(id := "messageList")(messageList()),
          div(id := "errorDiv", color.red),
          form(onsubmit := "return submitForm()")(
            input(`type` := "text", id := "replyIDInput", placeholder := "Reply To (Optional)"),
            input(`type` := "text", id := "nameInput", placeholder := "Username"),
            input(`type` := "text", id := "msgInput", placeholder := "Write a message!"),
            input(`type` := "submit", value := "Send")
          ),
          form(onsubmit := "return submitFilter()")(
            input(`type` := "text", id := "filterInput", placeholder := "Filter Messages"),
            input(`type` := "submit", value := "Filter"),
          )
        )
      )
    )
  )

  def messageList(nameFilter: Option[String] = None): generic.Frag[Builder, String] = {
    val filteredMsg = db.getMessages(nameFilter)
    val messagesGroupedByParent = filteredMsg.groupBy(_.idParent)

    def getChildren(groupedMessages: Map[String, List[Message]], depth: Int = 0, idParent: String = "none"): Frag = {
      val oneParentMsg = groupedMessages.get(idParent)
      if (oneParentMsg.isEmpty) return frag()
      for (message <- oneParentMsg.get) yield frag(renderMessage(message, depth), getChildren(groupedMessages, depth + 1, message.id))
    }

    def renderMessage(message: Message, tabs: Int = 0): generic.Frag[Builder, String] = {
      val Message(id, name, msg, t, _) = message
      p("\t"*tabs, "#", id, " ", b(name), " ", msg , " ", t)
    }
    if (nameFilter.isDefined) {
      frag(for (msg <- filteredMsg) yield renderMessage(msg))
    } else getChildren(messagesGroupedByParent)
  }

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }

  @cask.postJson("/")
  def postChatMsg(username: String, msg: String, idParent: String = "none"): ujson.Obj = {
    log.debug(username, msg)
    var idP = idParent
    if(idParent == "") {idP = "none"}
    if (username == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (username.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (idP != "none" && !db.getMessages().exists(_.id == idP)) ujson.Obj("success" -> false, "err" -> "There is no message to reply")
    else synchronized {
      val id = (db.getMessages().length + 1).toString()
      db.addMessage(id, username, msg, idP)
      connectionPool.sendAll(connection => Ws.Text(messageList(connectionPool.getFilter(connection)).render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.get("/stats/top")
  def getTopChatters(): ujson.Obj =
    ujson.Obj("top" -> db.getTop10Chatters.map(usrCount => {
      ujson.Obj(
        "username" -> usrCount._1,
        "count" -> usrCount._2,
      )
    }))

  @cask.postJson("/messages")
  def postMessage(username: String, msg: String, idParent: String = "none"): ujson.Obj = {
    if (username == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (username.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (idParent != "none" && !db.getMessages().exists(_.id == idParent)) ujson.Obj("success" -> false, "err" -> "There is no message to reply")
    else {
      val messageId = (db.getMessages().length + 1).toString()
      db.addMessage(
        messageId, username, msg, idParent
      )
      connectionPool.sendAll(connection => Ws.Text(messageList(connectionPool.getFilter(connection)).render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.get("/messages/:username")
  def getUserMessage(username: String): ujson.Obj =
    ujson.Obj(
      "username" -> username,
      "msg" -> db.getMessages(Some(username)).map(_.msg)
    )

  @cask.get("/messages")
  def getMessagesOrderedByDate(
      from: Option[Long] = None,
      to: Option[Long] = None
  ): ujson.Obj = ujson.Obj(
    "messages" -> db
      .getMessagesByDate(from, to)
      .map(message => {
        ujson.Obj(
          "id" -> message.id,
          "username" -> message.username,
          "msg" -> message.msg,
          "idParent" -> message.idParent
        )
      })
  )

  @cask.get("/messages/:username/stats")
  def getUserStats(username: String): ujson.Obj = ujson.Obj(
    "stats" -> ujson.Arr(ujson.Obj("count" -> db.getUserStats(username)))
  )

  log.debug(s"Starting at $serverUrl")
  initialize()
}
