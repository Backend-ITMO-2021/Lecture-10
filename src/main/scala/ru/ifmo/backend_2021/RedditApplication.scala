package ru.ifmo.backend_2021

import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.db.{DBAdapter, MessageDB}
import scalatags.Text.all._
import scalatags.generic
import scalatags.text.Builder
import java.util.Date
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.collection.immutable.ListMap

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
          h1("Reddit: Swain is mad :("),
          div(id := "messageList")(messageList()),
          div(id := "errorDiv", color.red),
          form(onsubmit := "return submitForm()")(
            input(`type` := "text", id := "nameInput", placeholder := "Username"),
            input(`type` := "text", id := "msgInput", placeholder := "Write a message!"),
            input(`type` := "text", id := "replyToInput", placeholder := "Reply to?"),
            input(`type` := "submit", value := "Send"),
          ),
          form(onsubmit := "return submitFilter()")(
            input(`type` := "text", id := "filterInput", placeholder := "Filter Messages"),
            input(`type` := "submit", value := "Submit"),
          )
        )
      )
    )
  )

  // def messageList(): generic.Frag[Builder, String] = frag(for (Message(name, msg) <- db.getMessages) yield p(b(name), " ", msg))

  def messageList(DBbuf: List[Message] = db.getMessages.filter(_.replyTo.getOrElse(0) == 0), lvl: Int = 0): generic.Frag[Builder, String] = {
    if (DBbuf.isEmpty) return frag()
    for (message <- DBbuf)
      yield frag(p("#", message.id, " ", b(message.username), " ", message.message, " ", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.date), ZoneId.of("UTC"))), marginLeft.:=(lvl*20)), messageList(db.getMessages.filter(_.replyTo.getOrElse(0) == message.id), lvl+1))
  }

  def filterMessageList(filter: Option[String] = None, DBbuf: List[Message] = db.getMessages, lvl: Int = 0): generic.Frag[Builder, String] = {
    if (filter.isEmpty || filter.getOrElse("") == ""){
      return messageList()
    }
    if (!DBbuf.exists(_.username == filter.get)) return frag()
    for (message <- DBbuf.filter(_.username == filter.get))
      yield frag(p("#", message.id, " ", b(message.username), " ", message.message, " ", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(message.date), ZoneId.of("UTC"))), marginLeft.:=(lvl*20)))
  }

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }

  @cask.postJson("/")
  def postChatMsg(name: String, msg: String, replyTo: String = ""): ujson.Obj = {
    log.debug(name, msg)
    if (name == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (name.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else synchronized {
      db.addMessage(Message(db.getMessages.length+1, name, msg, replyTo.toIntOption, new Date().getTime))
      connectionPool.sendAll(connection => Ws.Text(filterMessageList(connectionPool.getFilter(connection)).render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }
  @cask.get("/messages")
  def getAllMessages(): ujson.Obj = ujson.Obj(
    "messages" -> messagesToJson(db.getMessages)
  )
  @cask.get("/stats/top")
  def getTopCountUserMessages(): ujson.Obj = ujson.Obj(
    "top" -> chattersToJson(db.getTopUsers)
  )
  @cask.get("/messages/:user")
  def getAllUserMessages(user: String): ujson.Obj = ujson.Obj(
    "messages" -> messagesToJson(db.getUserMessages(user))
  )

  @cask.get("/messages/:user/stats")
  def getCountUserMessages(user: String): ujson.Obj = ujson.Obj(
    "stats" -> List(ujson.Obj("count" -> db.getCountMessages(user)))
  )

  @cask.postJson("/messages")
  def postMsg(from: Long = 0, to: Long = Long.MaxValue): List[ujson.Obj] = messagesToJson(db.dateFilterMessages(from, to))

  def chattersToJson(users: List[(String, Long)]): Iterable[ujson.Obj] = {
    for ((username, count) <- users) yield chatterToJson(username, count)
  }
  def chatterToJson(username: String, count: Long): ujson.Obj = ujson.Obj(
    "username" -> username,
    "count" -> count
  )
  def messagesToJson(messages: List[Message]): List[ujson.Obj] = {
    for (Message(id, username, msg, replyTo, date) <- messages) yield messageToJson(id, username, msg, replyTo.getOrElse("").toString, date)
  }

  def messageToJson(id: Int, username: String, msg: String, replyTo: String, date: Long): ujson.Obj = ujson.Obj(
    "id" -> id,
    "username" -> username,
    "message" -> msg,
    "replyTo" -> replyTo,
    "date" -> date
  )
  log.debug(s"Starting at $serverUrl")
  initialize()
}
