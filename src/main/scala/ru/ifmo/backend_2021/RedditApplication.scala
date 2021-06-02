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
  val db: MessageDB = new DBAdapter()
  val connectionPool: ConnectionPool = WsConnectionPool()

  @cask.staticResources("/static")
  def staticResourceRoutes() = "static"

  @cask.get("/")
  def hello(): Document = doctype("html")(
    html(
      head(
        link(rel := "stylesheet", href := ApplicationUtils.styles),
        script(src := "/static/app.js")
      ),
      body(
        div(cls := "container")(
          h1("Reddit: Thresh is mad :("),
          div(id := "messageList")(messageList()),
          div(id := "errorDiv", color.red),
          form(onsubmit := "return submitForm()")(
            input(
              `type` := "text",
              id := "parentInput",
              placeholder := "Reply (Optional)"
            ),
            input(
              `type` := "text",
              id := "nameInput",
              placeholder := "Username"
            ),
            input(
              `type` := "text",
              id := "msgInput",
              placeholder := "Write a message!"
            ),
            input(`type` := "submit", value := "Send")
          ),
          form(onsubmit := "return submitFilter()")(
            input(
              `type` := "text",
              id := "filterInput",
              placeholder := "Username"
            ),
            input(`type` := "submit", value := "Filter")
          )
        )
      )
    )
  )

  def messagesThread(
      parentMessages: Map[Option[Int], List[Message]],
      parentId: Option[Int] = None
  ): Frag = {
    val messages = parentMessages.get(parentId)
    if (messages.isEmpty) return frag()
    for (message <- messages.get)
      yield ul(
        li(s"#${message.id} ", b(message.username), " ", message.message),
        messagesThread(parentMessages, Some(message.id))
      )
  }

  def messageList(
      filter: Option[String] = None
  ): generic.Frag[Builder, String] = {
    if (filter.nonEmpty) messageFilter(filter.get)
    else messagesThread(db.getMessages.groupBy(_.parentId))
  }

  def messageFilter(username: String): generic.Frag[Builder, String] = {
    val messages = db.getUserMessages(username)
    for (message <- messages)
      yield ul(
        li(s"#${message.id} ", b(message.username), " ", message.message)
      )
  }

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }

  @cask.postJson("/")
  def postChatMsg(
      username: String,
      msg: String,
      parentId: String = ""
  ): ujson.Obj = {
    log.debug(username, msg)
    if (username == "")
      ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "")
      ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (username.contains("#"))
      ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (parentId != "" && (!db.getMessages.exists(_.id == parentId.toInt)))
      ujson.Obj("success" -> false, "err" -> "Reply message doesn't exist")
    else
      synchronized {
        val parentOption = parentId.toIntOption
        db.addMessage(
          username,
          msg,
          parentOption
        )
        connectionPool.sendAll(connection =>
          Ws.Text(
            messageList(connectionPool.getChannelFilter(connection)).render
          )
        )
        ujson.Obj("success" -> true, "err" -> "")
      }
  }

  @cask.postJson("/messages")
  def postMessage(
      username: String,
      message: String,
      replyTo: Int = 0
  ): ujson.Obj = {
    if (username == "")
      ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (message == "")
      ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (username.contains("#"))
      ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (replyTo != 0 && !db.getMessages.exists(_.id == replyTo))
      ujson.Obj("success" -> false, "err" -> "Reply message doesn't exist")
    else {
      db.addMessage(
        username,
        message,
        if (replyTo > 0) Option(replyTo) else None
      )
      connectionPool.sendAll(connection =>
        Ws.Text(
          messageList(connectionPool.getChannelFilter(connection)).render
        )
      )
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.get("/messages/:username")
  def getUserMessage(username: String): ujson.Obj = ujson.Obj(
    "username" -> username,
    "messages" -> db.getUserMessages(username).map(_.message)
  )

  @cask.get("/messages")
  def getMessages(
      from: Option[Long] = None,
      to: Option[Long] = None
  ): ujson.Obj = ujson.Obj(
    "messages" -> db
      .getMessagesByDate(from, to)
      .map(message => {
        ujson.Obj(
          "id" -> message.id,
          "username" -> message.username,
          "message" -> message.message,
          "parentId" -> message.parentId
        )
      })
  )

  @cask.get("/messages/:username/stats")
  def getUserStats(username: String): ujson.Obj = ujson.Obj(
    "stats" -> ujson.Arr(ujson.Obj("count" -> db.getUserStats(username)))
  )

  @cask.get("/stats/top")
  def getTopUsers(): ujson.Obj = ujson.Obj(
    "top" -> db.getTopUsers.map(v => {
      ujson.Obj(
        "username" -> v._1,
        "count" -> v._2
      )
    })
  )

  log.debug(s"Starting at $serverUrl")
  initialize()
}
