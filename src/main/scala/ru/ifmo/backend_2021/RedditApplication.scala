package ru.ifmo.backend_2021

import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.db.{DBAdapter, MessageDB}
import scalatags.Text.all._
import scalatags.generic
import scalatags.text.Builder

import java.text.DateFormat
import java.util.Date

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
            input(`type` := "text", id := "replyToInput", placeholder := "Reply to (message id)"),
            input(`type` := "submit", value := "Send"),
          ),
          form(onsubmit := "return applyFilter()")(
            input(`type` := "text", id := "filterInput", placeholder := "Username to filter by"),
            input(`type` := "submit", value := "Filter"),
          )
        )
      )
    )
  )

  def toInt(s: String): Int = {
    try {
      s.toInt
    } catch {
      case e: NumberFormatException => 0
    }
  }

  def messageList(filter: Option[String] = None): generic.Frag[Builder, String] = {
    def addFollowing(depth: Int, messages: List[Message]): generic.Frag[Builder, String] = {
      if (messages.nonEmpty) {
        for (message <- messages)
          yield frag(p(css("white-space") := "pre", "   ".repeat(depth), "#", message.id, " ", b(message.username), " ", message.message),
          addFollowing(depth + 1, db.getMessages(filter.getOrElse("")).filter(_.replyTo == Option(message.id))))
      }
    }

    addFollowing(0, db.getMessages(filter.getOrElse("")).filter(_.replyTo.isEmpty))
  }

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }

  @cask.postJson("/")
  def postChatMsg(name: String, msg: String, replyTo: String = ""): ujson.Obj = {
    log.debug(name, msg, replyTo)
    if (name == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (name.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (replyTo.contains("#")) ujson.Obj("success" -> false, "err" -> "Id to replying must be without '#'")
    else if (toInt(replyTo) > db.getMessages.length) ujson.Obj("success" -> false, "err" -> "Message with given id to replying doesn't exist")
    else synchronized {
      db.addMessage(Message(db.getMessages.length + 1, new Date(), name, msg, replyTo.toIntOption))
      connectionPool.sendAll(connection => Ws.Text(messageList(connectionPool.getFilter(connection)).render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  log.debug(s"Starting at $serverUrl")
  initialize()
}
