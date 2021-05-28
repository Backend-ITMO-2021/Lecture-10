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

      assert(success.text().contains("Scala Reddit"))
      assert(success.statusCode == 200)

      val wsMsg = Await.result(wsPromise.future, Inf)

      wsPromise = scala.concurrent.Promise[String]
      requests.post(host, data = ujson.Obj("username" -> "ilya", "message" -> "Test Message 1!"))
      wsPromise = scala.concurrent.Promise[String]
      requests.post(host, data = ujson.Obj("username" -> "ilya", "message" -> "Test Message 2!"))
      wsPromise = scala.concurrent.Promise[String]
      val response = requests.post(host, data = ujson.Obj("username" -> "ilya", "message" -> "Test Message 3!"))

      val parsed = ujson.read(response)
      assert(parsed("success") == ujson.True)
      assert(parsed("err") == ujson.Str(""))

      val success2 = requests.get(host)
      assert(success2.text().contains("Test Message 1!"))
      assert(success2.text().contains("Test Message 2!"))
      assert(success2.text().contains("Test Message 3!"))

      val wsMsg2 = Await.result(wsPromise.future, Inf)
      assert(wsMsg2.contains("Test Message 1!"))
      assert(wsMsg2.contains("Test Message 2!"))
      assert(wsMsg2.contains("Test Message 3!"))

      // Nesting and Auto Id's
      assert(success2.text().contains("#1"))
      assert(success2.text().contains("#2"))
      assert(success2.text().contains("#3"))

      wsPromise = scala.concurrent.Promise[String]
      requests.post(host, data = ujson.Obj("username" -> "test", "message" -> "Test Reply!", "replyTo" -> "1"))

      val success3 = requests.get(host)
      assert(success3.text().contains("#4"))
      assert(success3.text().indexOf("#4") < success3.text().indexOf("#3"))

      val wsMsg3 = Await.result(wsPromise.future, Inf)
      assert(wsMsg3.contains("#4"))
      assert(wsMsg3.indexOf("#4") < wsMsg3.indexOf("#3"))

      // Messages Filtering
      wsPromise = scala.concurrent.Promise[String]
      wsClient.send(cask.Ws.Text("test"))

      val wsMsg4 = Await.result(wsPromise.future, Inf)
      assert(wsMsg4.contains("test"))
      assert(!wsMsg4.contains("ilya"))
    }

    test("failure") - withServer(RedditApplication) { host =>
      val response1 = requests.post(host, data = ujson.Obj("username" -> "ilya"), check = false)
      assert(response1.statusCode == 400)
      val response2 = requests.post(host, data = ujson.Obj("username" -> "ilya", "message" -> ""))
      assert(
        ujson.read(response2) ==
          ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
      )
      val response3 = requests.post(host, data = ujson.Obj("username" -> "", "message" -> "Test Message!"))
      assert(
        ujson.read(response3) ==
          ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
      )
      val response4 = requests.post(host, data = ujson.Obj("username" -> "123#123", "message" -> "Test Message!"))
      assert(
        ujson.read(response4) ==
          ujson.Obj("success" -> false, "err" -> "Username cannot contain '#'")
      )
    }

    test("javascript") - withServer(RedditApplication) { host =>
      val response1 = requests.get(host + "/static/app.js")
      assert(response1.text().contains("function submitForm()"))
    }
  }
}