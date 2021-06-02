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
            input(`type` := "number", id := "replyInput", placeholder := "Reply To (Optional)"),
            input(`type` := "text", id := "nameInput", placeholder := "Username"),
            input(`type` := "text", id := "msgInput", placeholder := "Write a message!"),
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

  def messageList(filter: Option[String] = None): generic.Frag[Builder, String] = {
    def nextMessage(sortedMessages: Map[Option[Int], List[Message]], indents: Int, parent: Option[Int]): generic.Frag[Builder, String] = {
      val messages = sortedMessages.get(parent)
      if (messages.isDefined) {
        for (message <- messages.get) yield frag(
          p(css("white-space") := "pre", List.fill(indents)("    ").mkString + "#", message.id, " ", b(message.username), " ", message.message, " [",  DateFormat.getDateTimeInstance().format(message.date), "]"),
          nextMessage(sortedMessages, indents + 1, Some(message.id)))
      }
      else
        frag()

    }

    def filterMessage(messages: List[Message], name: String): generic.Frag[Builder, String] = {
      val filteredMessages = messages.filter(_.username == name)

      if (filteredMessages.nonEmpty)
        for (message <- filteredMessages)
          yield frag(p("#", message.id, " ", b(message.username), " ", message.message, " [", DateFormat.getDateTimeInstance().format(message.date), "]"))
      else
        frag()

    }

    if (filter.isDefined & filter.getOrElse("") != "") {
      filterMessage(db.getMessages, filter.get.trim)
    } else {
      val sortedMessages = db.getMessages.groupBy(_.parent)
      nextMessage(sortedMessages, 0, None)
    }

  }

  @cask.websocket("/subscribe")
  def subscribe(): WsHandler = connectionPool.wsHandler { connection =>
    connectionPool.send(Ws.Text(messageList().render))(connection)
  }

  @cask.postJson("/")
  def postChatMsg(name: String, msg: String, parent: String =""): ujson.Obj = {
    log.debug(name, msg, parent)
    if (name == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (name.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (parent.contains("#")) ujson.Obj("success" -> false, "err" -> "Reply To cannot contain '#'")
    else synchronized {
      db.addMessage(Message(db.getMessages.length + 1, name, msg, parent.toIntOption, System.currentTimeMillis()))
      connectionPool.sendAll(connection => Ws.Text(messageList(connectionPool.getFilter(connection)).render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.get("/messages")
  def getMessages(from: Long = 0, to: Long = Long.MaxValue): ujson.Obj =
    ujson.Obj("messages" -> db.getMessagesOrderedByDate(from, to).map(message => {
      ujson.Obj(
        "id" -> message.id,
        "username" -> message.username,
        "message" -> message.message,
        "replyTo" -> message.parent,
        "date" -> message.date
      )
    }))

  @cask.get("/top")
  def getTopChatters(): ujson.Obj =
    ujson.Obj("top" -> db.getTop10Chatters.map(top => {
      ujson.Obj(
        "username" -> top._1,
        "count" -> top._2,
      )
    }))

  @cask.get("/messages/:username")
  def getUserMessages(username: String): ujson.Obj =
    ujson.Obj("messages" -> db.getMessagesByUser(username))

  @cask.get("/messages/:username/stats")
  def getCountUserMessages(username: String): ujson.Obj =
    ujson.Obj("stats" -> List("count" -> db.getMessagesByUser(username).length))


  log.debug(s"Starting at $serverUrl")
  initialize()
}
