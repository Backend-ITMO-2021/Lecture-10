import TestUtils.withServer
import ru.ifmo.backend_2021.RedditApplication
import utest._
import castor.Context.Simple.global
import cask.util.Logger.Console._

import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf

object RedditTest extends TestSuite {
  val tests: Tests = Tests {
    test("success") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }
      val success = requests.get(host)

      assert(success.text().contains("Reddit: Swain is mad :("))
      assert(success.text().contains("test"))
      assert(success.text().contains("First message!"))
      assert(success.text().contains("willy"))
      assert(success.text().contains("I'm here by chance."))
      assert(success.statusCode == 200)

      val wsMsg = Await.result(wsPromise.future, Inf)
      assert(wsMsg.contains("test"))
      assert(wsMsg.contains("First message!"))
      assert(wsMsg.contains("willy"))
      assert(wsMsg.contains("I'm here by chance."))

      wsPromise = scala.concurrent.Promise[String]
      val response = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message!"))

      val parsed = ujson.read(response)
      assert(parsed("success") == ujson.True)
      assert(parsed("err") == ujson.Str(""))

      assert(response.statusCode == 200)
      val wsMsg2 = Await.result(wsPromise.future, Inf)
      assert(wsMsg.contains("test"))
      assert(wsMsg.contains("First message!"))
      assert(wsMsg.contains("willy"))
      assert(wsMsg.contains("I'm here by chance."))
      assert(wsMsg2.contains("ilya"))
      assert(wsMsg2.contains("Test Message!"))

      val success2 = requests.get(host)
      assert(success.text().contains("test"))
      assert(success.text().contains("First message!"))
      assert(success.text().contains("willy"))
      assert(success.text().contains("I'm here by chance."))
      assert(success2.text().contains("ilya"))
      assert(success2.text().contains("Test Message!"))
      assert(success2.statusCode == 200)
    }
    test("failure") - withServer(RedditApplication) { host =>
      val response1 = requests.post(host, data = ujson.Obj("name" -> "ilya"), check = false)
      assert(response1.statusCode == 400)
      val response2 = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> ""))
      assert(
        ujson.read(response2) ==
          ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
      )
      val response3 = requests.post(host, data = ujson.Obj("name" -> "", "msg" -> "Test Message!"))
      assert(
        ujson.read(response3) ==
          ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
      )
      val response4 = requests.post(host, data = ujson.Obj("name" -> "123#123", "msg" -> "Test Message!"))
      assert(
        ujson.read(response4) ==
          ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
      )
    }

    test("javascript") - withServer(RedditApplication) { host =>
      val response1 = requests.get(host + "/static/app.js")
      assert(response1.text().contains("function submitForm()"))
      assert(response1.text().contains("function submitFilter()"))
    }

    test("IDs cascade") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }

      val success = requests.get(host)

      assert(success.text().contains("#1"))
      assert(success.text().contains("#2"))
      assert(success.text().contains("#3"))
      assert(success.statusCode == 200)

      wsPromise = scala.concurrent.Promise[String]
      val response = requests.post(host, data = ujson.Obj("name" -> "name", "msg" -> "msg", "parent" -> "1"))

      val parsed = ujson.read(response)
      assert(parsed("success") == ujson.True)
      assert(parsed("err") == ujson.Str(""))

      val success2 = requests.get(host)

      assert(success2.text().contains("#4"))
      assert(success2.text().indexOf("#4") < success2.text().indexOf("#2"))
    }

    test("Filter") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }

      wsPromise = scala.concurrent.Promise[String]
      wsClient.send(cask.Ws.Text("willy"))

      val wsMsg = Await.result(wsPromise.future, Inf)

      assert(wsMsg.contains("#2"))
      assert(wsMsg.contains("willy"))
    }

    test("API") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }

      val allMessages = requests.get(host + "/messages")
      val allMessagesParsed = ujson.read(allMessages)
      assert(allMessagesParsed("messages").arr.length == 4)

      val topChatters = requests.get(host + "/top")
      val topChattersParsed = ujson.read(topChatters)
      assert(topChattersParsed("top").arr.length == 4)

      val userMessages = requests.get(host + "/messages/willy")
      val userMessagesParsed = ujson.read(userMessages)
      assert(userMessagesParsed("messages").arr.length == 1)
      assert(userMessages.text().contains("I'm here by chance."))

      val userStatsMessages = requests.get(host + "/messages/willy/stats")
      assert(userStatsMessages.text().contains("1"))
    }
  }
}