package ru.ifmo.backend_2021

import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.db.{DBAdapter, MessageDB}
import scalatags.Text.all._
import scalatags.Text.tags2
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
        tags2.title("Scala Reddit"),
        link(rel := "stylesheet", href := ApplicationUtils.styles),
        link(rel := "icon", `type` := "image/png", href := "/static/favicon.png"),
        script(src := "/static/app.js")
      ),
      body(
        div(cls := "container border border-2 border-primary my-5 p-5", css("max-width") := "700px", css("border-radius") := "10px")(
          h1(cls := "mb-5")("Scala Reddit"),

          div(id := "messageList", cls := "mb-5")(messageList()),

          div(cls := "mb-3 text-danger fw-bold", id := "errorDiv"),

          form(onsubmit := "return submitForm()")(
            div(cls := "input-group")(
              input(`type` := "text", id := "replyInput", cls := "form-control", placeholder := "Reply (Optional)"),
              input(`type` := "text", id := "nameInput", cls := "form-control", placeholder := "Username"),
              input(`type` := "text", id := "msgInput", cls := "form-control", placeholder := "Write a message!"),
              button(`type` := "submit", cls := "btn btn-primary", "Send")
            )
          ),

          form(onsubmit := "return submitFilter()")(
            div(cls := "input-group mt-3")(
              input(`type` := "text", id := "filterInput", cls := "form-control", placeholder := "Filter By User"),
              button(`type` := "submit", cls := "btn btn-secondary", "Filter")
            )
          )
        )
      )
    )
  )

  def messageList(filter: Option[String] = None): generic.Frag[Builder, String] = {
    val messages = db.getMessages(filter)

    def buildMessageThread(groupedMessages: Map[Option[Int], List[Message]], parentId: Option[Int] = None, depth: Int = 0): Frag = {
      val messages = groupedMessages.get(parentId)

      if (messages.isEmpty) {
        frag()
      } else {
        for (message <- messages.get)
          yield frag(renderMessage(message, depth), buildMessageThread(groupedMessages, Some(message.id), depth + 1))
      }
    }

    def renderMessage(message: Message, depth: Int = 0): generic.Frag[Builder, String] = {
      val Message(id, name, msg, t, _) = message
      p(span(css("white-space") := "pre-wrap", "    " * depth), span(cls := "text-secondary", s"#$id"), " ", b(name), " ", msg, " ", span(cls := "text-secondary", t))
    }

    if (filter.isDefined) {
      frag(for (msg <- messages) yield renderMessage(msg))
    } else {
      val messagesGroupedByRoot = messages.groupBy(_.replyTo)
      buildMessageThread(messagesGroupedByRoot)
    }
  }


  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }

  @cask.postJson("/")
  def postChatMsg(username: String, message: String, replyTo: String = ""): ujson.Obj = {
    log.debug("WebView: / (POST) ", username, message, replyTo)

    if (username == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (message == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (username.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (replyTo.contains("#")) ujson.Obj("success" -> false, "err" -> "Reply To cannot contain '#'")
    else synchronized {
      db.addMessage(username, message, replyTo.toIntOption)
      connectionPool.sendAll(connection => Ws.Text(messageList(connectionPool.getFilter(connection)).render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  log.debug(s"Starting at $serverUrl")
  initialize()
}

