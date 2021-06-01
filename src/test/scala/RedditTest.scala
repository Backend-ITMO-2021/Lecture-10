import TestUtils.withServer
import ru.ifmo.backend_2021.RedditApplication
import utest._
import castor.Context.Simple.global
import cask.util.Logger.Console._

import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf

object RedditTest extends TestSuite {
  var username = "testUser1"

  val tests: Tests = Tests {
    test("success") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe/$username") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }
      val authPage = requests.get(host)

      assert(authPage.text().contains("Welcome user"))
      assert(authPage.text().contains("Please enter your nickname"))
      assert(authPage.statusCode == 200)

      val chatPage = requests.get(s"$host/users/$username")
      assert(chatPage.text().contains(s"Welcome: $username"))
      assert(chatPage.statusCode == 200)

      val wsMsg = Await.result(wsPromise.future, Inf)
      assert(wsMsg.contains("No messages"))

      wsPromise = scala.concurrent.Promise[String]
      val response = requests.post(host, data = ujson.Obj("to" -> "", "name" -> "ilya", "msg" -> "Test Message!"))

      val parsed = ujson.read(response)
      assert(parsed("success") == ujson.True)
      assert(parsed("err") == ujson.Str(""))

      assert(response.statusCode == 200)
      val wsMsg2 = Await.result(wsPromise.future, Inf)
      assert(wsMsg2.contains("ilya"))
      assert(wsMsg2.contains("Test Message!"))
    }
    test("failure") - withServer(RedditApplication) { host =>
      val response1 = requests.post(host, data = ujson.Obj("name" -> "ilya"), check = false)
      assert(response1.statusCode == 400)
      val response2 = requests.post(host, data = ujson.Obj("to" -> "", "name" -> "ilya", "msg" -> ""))
      assert(
        ujson.read(response2) ==
          ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
      )
      val response3 = requests.post(host, data = ujson.Obj("to" -> "", "name" -> "", "msg" -> "Test Message!"))
      assert(
        ujson.read(response3) ==
          ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
      )
      val response4 = requests.post(host, data = ujson.Obj("to" -> "", "name" -> "123#123", "msg" -> "Test Message!"))
      assert(
        ujson.read(response4) ==
          ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
      )
    }

    test("javascript") - withServer(RedditApplication) { host =>
      val response1 = requests.get(host + "/static/app.js")
      assert(response1.text().contains("function submitForm()"))
    }

    test("API") - withServer(RedditApplication) {host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe/$username") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }
      val responseGetAllMessages = requests.get(host + "/messages")
      assert(responseGetAllMessages.statusCode == 200)


      val responseGetMessagesCountByUser = requests.get(host + "/messages/nikita1/stats")
      assert(responseGetMessagesCountByUser.statusCode == 200)
      assert(responseGetMessagesCountByUser.text().contains("0"))

      val responseTopChatters = requests.get(host + "/messages/stats/top")
      assert(responseTopChatters.statusCode == 200)
      assert(responseTopChatters.text().contains("ilya"))

      val responseAddMessage = requests.post(host + "/messages", data = ujson.Obj("to" -> "", "name" -> "nikita", "msg" -> "hello"))
      assert(responseAddMessage.statusCode == 200)
      assert(requests.get(host + "/messages").text().contains("hello"))

      val responseBadAddMessage = requests.post(host + "/messages", data = ujson.Obj("to" -> "", "name" -> "", "msg" -> "hello"))
      assert(
        ujson.read(responseBadAddMessage) ==
          ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
      )
    }
  }
}