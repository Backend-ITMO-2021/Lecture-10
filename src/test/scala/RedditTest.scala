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
      assert(success.statusCode == 200)

      wsPromise = scala.concurrent.Promise[String]
      val response = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message!"))

      val parsed = ujson.read(response)
      assert(parsed("success") == ujson.True)
      assert(parsed("err") == ujson.Str(""))

      assert(response.statusCode == 200)
      val wsMsg = Await.result(wsPromise.future, Inf)
      assert(wsMsg.contains("ilya"))
      assert(wsMsg.contains("Test Message!"))

      val success2 = requests.get(host)
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
      val response5 = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message!", "replyTo" -> "#2"))
      assert(
        ujson.read(response5) ==
          ujson.Obj("success" -> false, "err" -> "Id to replying must be without '#'")
      )
      val response6 = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message!", "replyTo" -> "-1"))
      assert(
        ujson.read(response6) ==
          ujson.Obj("success" -> false, "err" -> "Message with given id to replying doesn't exist")
      )
    }
    test("task2") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }

      val response1 = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "First Message"))
      val response2 = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Second Message"))
      val response3 = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Third Message"))
      assert(response1.statusCode == 200)
      assert(response2.statusCode == 200)
      assert(response3.statusCode == 200)

      val success = requests.get(host)
      assert(success.text().contains("#1"))
      assert(success.text().contains("#2"))
      assert(success.text().contains("#3"))
      assert(success.statusCode == 200)

      val response = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Reply Message", "replyTo" -> "2"))
      assert(response.statusCode == 200)
      val success1 = requests.get(host)
      assert(success1.text().contains("   #5"))
      assert(success1.statusCode == 200)
      assert(success1.text().indexOf("#5") < success1.text().indexOf("#3"))
      assert(success1.text().indexOf("#5") > success1.text().indexOf("#2"))
    }
    test("task3") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }

      val response = requests.post(host, data = ujson.Obj("name" -> "panda", "msg" -> "buuu"))

      val allMessages = requests.get(host + "/messages")
      val allMessagesParsed = ujson.read(allMessages)
      assert(allMessagesParsed("messages").arr.length == 6)

      val topUsers = requests.get(host + "/stats/top")
      val topUsersParsed = ujson.read(topUsers)
      assert(topUsersParsed("top").arr.length == 2)
      assert(topUsers.text().contains("ilya"))
      assert(topUsers.text().contains("panda"))


      val userMessages = requests.get(host + "/messages/ilya")
      val userMessagesParsed = ujson.read(userMessages)
      assert(userMessagesParsed("messages").arr.length == 5)

      val userMessagesCount = requests.get(host + "/messages/ilya/stats")
      assert(userMessagesCount.text().contains("5"))
    }
    test("javascript") - withServer(RedditApplication) { host =>
      val response1 = requests.get(host + "/static/app.js")
      assert(response1.text().contains("function submitForm()"))
      assert(response1.text().contains("function applyFilter()"))
    }
  }
}