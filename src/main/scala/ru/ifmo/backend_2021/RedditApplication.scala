package ru.ifmo.backend_2021

import cask.endpoints.WsHandler
import cask.util.Ws
import ru.ifmo.backend_2021.ApplicationUtils.Document
import ru.ifmo.backend_2021.connections.{ConnectionPool, WsConnectionPool}
import ru.ifmo.backend_2021.db.{DBAdapter, MessageDB}
import scalatags.Text.all._
import scalatags.{Text, generic}
import scalatags.text.Builder
import ujson.Obj

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object RedditApplication extends cask.MainRoutes {
  val serverUrl = s"http://$host:$port"
  val db: MessageDB = new DBAdapter()
//  db.addMessage(new MessageDTO(None, "testUser1", "Hello!"))
//  db.addMessage(new MessageDTO(None, "testUser2", "Hi!"))
//  db.addUser(AppUserDTO("ivan"))
  val connectionPool: ConnectionPool = WsConnectionPool()

  @cask.staticResources("/static")
  def staticResourceRoutes() = "static"

  @cask.get("/")
  def authorizationPage(): Document = doctype("html")(
    html(
      head(
        link(rel := "stylesheet", href := ApplicationUtils.styles),
        script(src := "/static/app.js")
      ),
      body(
        div(cls := "container", display := "flex", justifyContent := "center",
          marginTop := "20px"
        )(
          div(width := "50%", display := "flex", flexDirection := "column", alignItems := "center",
            borderRadius := "9px",
            border := "2px solid black",
            paddingTop := "15px",
            paddingBottom := "15px"
          )(
            div(
              h3("Welcome user!")
            ),
            div(
              h5("Please enter your nickname")
            ),
            div(marginTop := "15px")(id := "errorDiv", color.red),
            div(
              form(onsubmit := "return authForm()")(
                input(`type` := "text", id := "authNameInput", placeholder := "Username"),
                input(`type` := "submit", value := "Connect"),
              )
            )
          )
        )
      )
    )
  )

  @cask.postJson("/auth")
  def auth(userNickname: String): ujson.Obj = {
    log.debug("Auth endpoint", userNickname)
    if (userNickname == "") ujson.Obj("success" -> false, "err" -> "Nickname cannot be empty")
    else if (userNickname.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else ujson.Obj("success" -> true, "err" -> "")
  }

  @cask.get("/users/:nickName")
  def hello(nickName: String): Document = {
    println(s"Cask.get -> hello, $nickName")

    doctype("html")(
      html(
        head(
          link(rel := "stylesheet", href := ApplicationUtils.styles),
          script(src := "/static/app.js")
        ),
        body(
          div(cls := "container", display := "flex", justifyContent := "center", marginTop := "25px",
            borderRadius := "9px",
            border := "2px solid black",
            width := "45%",
            paddingBottom := "16px"
          )(
            div(cls := "chat-container", display := "flex", flexDirection := "column", alignItems := "center", flexBasis := 2)(
              h2(s"Welcome: $nickName!"),
              div(style := "margin-top: 16px;")(
                form(onsubmit := "return filterForm()", display := "flex", justifyContent := "space-between")(
                  h4(i("User filter:")),
                  input(`type` := "text", id := "filterInput", placeholder := "Enter user name"),
                  input(`type` := "submit", value := "Apply"),
                )
              ),
              div(id := "messageList", style := "margin-top: 27px;")(
                messageList(db.getMessages)
              ),
              div(id := "errorDiv", color.red),
              form(onsubmit := "return submitForm()", display := "block", marginRight := "10px")(
                input(`type` := "text", id := "toInput", placeholder := "Reply to (can be empty)"),
                input(`type` := "text", id := "msgInput", placeholder := "Write a message!"),
                input(`type` := "submit", value := "Send", marginTop := "10px", display :="block", width := "100%",
                  backgroundColor := "#8BC34A")
              )
            ),
            div(cls := "aside", borderLeft := "2px solid black", paddingLeft := "10px", flexBasis := 1)(
              div(id := "displayMode")(displayMode()),
              form(onsubmit := "return changeDisplayForm()")(
                input(`type` := "submit", id := "cascade", value := "Change display mode")
              )
            )
          )
        )
      )
    )
  }

  @cask.websocket("/subscribe/:userName")
  def subscribe(userName: String): WsHandler = connectionPool.wsHandler{ connection =>
    connectionPool.send(Ws.Text(messageList(db.getMessages).render))(connection)
  }(userName)

  def renderList(): ujson.Obj = {
    connectionPool.sendAll(connection => {
      val connParams = connectionPool.getConnectionUser(connection).getOrElse(AppUser(1, "errorUser", None, isCascade = false))
      Ws.Text(messageList(userFilter(connParams.userFilter), connParams.isCascade).render)
    })
    ujson.Obj("success" -> true, "err" -> "")
  }

  @cask.postJson("/")
  def postChatMsg(to: String, name: String, msg: String): ujson.Obj = {
    log.debug(name, msg)
    if (name == "") ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "") ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else if (name.contains("#") || to.contains("#")) ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
    else if (to == "_") ujson.Obj("success" -> false, "err" -> "Target user cannot be '_'")
    else synchronized {
      db.getUserByNickname(to) match {
        case Some(toUser) =>
          db.addMessage(MessageDTO(Some(toUser.id), name, msg)) //todo
          renderList()
          ujson.Obj("success" -> true, "err" -> "")
        case None => ujson.Obj("success" -> false, "err" -> "No such replyTo user")
      }
    }
  }

  def userFilter(filter: Option[String]): List[Message] = filter match {
    case Some(filterValue) => db.getMessages.filter(msg => msg.username.contains(filterValue))
    case None => db.getMessages
  }

  def messageList(messages: List[Message], isCascade: Boolean = false): generic.Frag[Builder, String] = {
    if (messages.isEmpty) {
      frag("No messages");
    } else {
      isCascade match {
        case false => frag(for (Message(id, replyTo, name, msg, date) <- messages) yield div(
          borderBottom := "1px solid grey",
          padding:= "5px 0"
        )(
          i(s"#$id"), " ",
          if(replyTo.isDefined) s"-> #${replyTo.get}" else "  ",
          "   ",  b(name),
          " ", msg,
          " ", date)
        )
        case true =>
          val withoutTo = mutable.LinkedHashMap[Int, Message]()
          val replies = mutable.HashMap[Int, ListBuffer[Message]]()
          messages.foreach(message => {
            message.replyTo match {
              case Some(to_id) => replies.get(to_id) match {
                case Some(l) => l.addOne(message)
                case None => replies.addOne(to_id, ListBuffer().addOne(message))
              }
              case None => withoutTo.addOne(message.id, message)
            }
          })

          val msgListBuilder = new ListBuffer[Text.TypedTag[String]]
          def addReply(msg_id: Int, repliesMap: mutable.HashMap[Int, ListBuffer[Message]], sb: ListBuffer[Text.TypedTag[String]], k: Int = 0): Unit = {
            repliesMap.get(msg_id) match {
              case Some(repList) => repList.foreach(reply => {
                sb.addOne(div(style := "margin-bottom: 18px")(for (_ <- 0 until k + 1) yield "-",  reply.toListItemStr))
                repliesMap.get(reply.id) match {
                  case Some(_) => addReply(reply.id, repliesMap, sb, k + 1)
                  case None => ()
                }
              })
              case None => ()
            }
          }
          withoutTo.foreach(withoutMsg => {
            msgListBuilder.addOne(div(style := "margin-bottom: 18px")(withoutMsg._2.toListItemStr))
            addReply(withoutMsg._1, replies, msgListBuilder)
          })
          frag(msgListBuilder.toList)
      }
    }
  }

  def displayMode(): generic.Frag[Builder, String] = {
    frag(h3(s"Display: Cascade"))
  }

  // ------------------ API -------------------------
  def messagesToJSON(messages: List[Message]): List[ujson.Obj] = for (Message(id, replyTo, username, msg, date) <- messages) yield ujson.Obj("id" -> id, "username" -> username, "msg" -> msg, "date" -> date)

  @cask.get("/messages")
  def getAllMessages(): Obj = ujson.Obj(
    "messages" -> messagesToJSON(db.getMessages)
  )

  @cask.get("/messages/:user")
  def getAllUserMessages(user: String): Obj = ujson.Obj(
    "messages" -> messagesToJSON(db.getMessages.filter(msg => msg.username.contains(user)))
  )

  @cask.postJson("/messages")
  def postMsg(to: String, name: String, msg: String): Obj = postChatMsg(to, name, msg)

  //  ---------------- INITIALIZE --------------------
  log.debug(s"Starting at $serverUrl")
  initialize()
}
