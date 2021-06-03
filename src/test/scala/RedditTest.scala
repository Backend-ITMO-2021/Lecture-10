import TestUtils.withServer
import ru.ifmo.backend_2021.RedditApplication
import utest._
import castor.Context.Simple.global
import cask.util.Logger.Console._
import cask.Ws

import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf

object RedditTest extends TestSuite {
  val tests: Tests = Tests {
    test("success") - withServer(RedditApplication) { host =>
      requests.post(host, data = ujson.Obj("name" -> "ventus976", "msg" -> "I don't particularly care which interaction they pick so long as it's consistent."))
      requests.post(host, data = ujson.Obj("name" -> "ventus976", "msg" -> "I do particularly care which interaction they pick so long as it's consistent."))
      requests.post(host, data = ujson.Obj("name" -> "ventus976", "msg" -> "I don't particularly care."))
      requests.post(host, data = ujson.Obj("name" -> "XimbalaHu3", "msg" -> "Exactly, both is fine but do pick one."))
      requests.post(host, data = ujson.Obj("name" -> "XimbalaHu3", "msg" -> "Exactly, both is fine but do pick two."))
      requests.post(host, data = ujson.Obj("name" -> "3", "msg" -> "3"))
      requests.post(host, data = ujson.Obj("name" -> "4", "msg" -> "4"))
      requests.post(host, data = ujson.Obj("name" -> "5", "msg" -> "5"))
      requests.post(host, data = ujson.Obj("name" -> "6", "msg" -> "6"))
      requests.post(host, data = ujson.Obj("name" -> "7", "msg" -> "7"))
      requests.post(host, data = ujson.Obj("name" -> "8", "msg" -> "8"))
      requests.post(host, data = ujson.Obj("name" -> "9", "msg" -> "9"))
      requests.post(host, data = ujson.Obj("name" -> "10", "msg" -> "10"))
      requests.post(host, data = ujson.Obj("name" -> "3", "msg" -> "3"))
      requests.post(host, data = ujson.Obj("name" -> "4", "msg" -> "4"))
      requests.post(host, data = ujson.Obj("name" -> "5", "msg" -> "5"))
      requests.post(host, data = ujson.Obj("name" -> "6", "msg" -> "6"))
      requests.post(host, data = ujson.Obj("name" -> "7", "msg" -> "7"))
      requests.post(host, data = ujson.Obj("name" -> "8", "msg" -> "8"))
      requests.post(host, data = ujson.Obj("name" -> "9", "msg" -> "9"))
      requests.post(host, data = ujson.Obj("name" -> "10", "msg" -> "10"))
      requests.post(host, data = ujson.Obj("name" -> "11", "msg" -> "11"))

      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }
      wsPromise = scala.concurrent.Promise[String]

      val wsMsg = Await.result(wsPromise.future, Inf)
      assert(wsMsg.contains("ventus976"))
      assert(wsMsg.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(wsMsg.contains("#2"))
      assert(wsMsg.contains("XimbalaHu3"))
      assert(wsMsg.contains("Exactly, both is fine but do pick one."))


      val success = requests.get(host)

      assert(success.text().contains("Reddit: Swain is mad :("))
      assert(success.text().contains("ventus976"))
      assert(success.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(success.text().contains("XimbalaHu3"))
      assert(success.text().contains("Exactly, both is fine but do pick one."))
      assert(success.statusCode == 200)


      wsPromise = scala.concurrent.Promise[String]
      val response = requests.post(host, data = ujson.Obj("name" -> "ilya", "msg" -> "Test Message!"))

      val parsed = ujson.read(response)
      assert(parsed("success") == ujson.True)
      assert(parsed("err") == ujson.Str(""))

      assert(response.statusCode == 200)
      val wsMsg2 = Await.result(wsPromise.future, Inf)
      assert(wsMsg2.contains("ventus976"))
      assert(wsMsg2.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(wsMsg2.contains("XimbalaHu3"))
      assert(wsMsg2.contains("Exactly, both is fine but do pick one."))
      assert(wsMsg2.contains("ilya"))
      assert(wsMsg2.contains("Test Message!"))

      val success2 = requests.get(host)
      assert(success2.text().contains("ventus976"))
      assert(success2.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(success2.text().contains("XimbalaHu3"))
      assert(success2.text().contains("Exactly, both is fine but do pick one."))
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
    }

    test("filter") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]
      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }
      val success = requests.get(host)
      wsPromise = scala.concurrent.Promise[String]
      wsClient.send(Ws.Text("ventus976"))
      val filter = Await.result(wsPromise.future, Inf)

      assert(filter.contains("ventus976"))
      assert(filter.contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(filter.contains("I do particularly care which interaction they pick so long as it's consistent."))
      assert(filter.contains("I don't particularly care."))
    }
    test("api_test") - withServer(RedditApplication) { host =>
      var wsPromise = scala.concurrent.Promise[String]

      val wsClient = cask.util.WsClient.connect(s"$host/subscribe") {
        case cask.Ws.Text(msg) => wsPromise.success(msg)
      }

      val getCurMessages = requests.get(host + "/messages/ventus976")
      assert(getCurMessages.text().contains("ventus976"))
      assert(getCurMessages.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(getCurMessages.statusCode == 200)

      val getCountVentusMessages = requests.get(host + "/messages/ventus976/stats")
      assert(getCountVentusMessages.text().contains("3"))
      assert(getCountVentusMessages.statusCode == 200)

      val getCountXimbalaHu3Messages = requests.get(host + "/messages/XimbalaHu3/stats")
      assert(getCountXimbalaHu3Messages.text().contains("2"))
      assert(getCountXimbalaHu3Messages.statusCode == 200)



      val getTopStats = requests.get(host + "/stats/top")
      assert(getTopStats.text().contains("XimbalaHu3"))
      assert(getTopStats.text().contains("4"))
      assert(getTopStats.text().contains("5"))
      assert(getTopStats.text().contains("6"))
      assert(getTopStats.text().contains("9"))
      assert(getTopStats.text().contains("10"))
      assert(!getTopStats.text().contains("11"))
      assert(getTopStats.statusCode == 200)

      val fromToResponse = requests.post(host + "/messages", data = ujson.Obj("from" -> "1622708224000", "to" -> "1632708224000"))
      assert(fromToResponse.text().contains("ventus976"))
      assert(fromToResponse.text().contains("I don't particularly care which interaction they pick so long as it's consistent."))
      assert(fromToResponse.text().contains("XimbalaHu3"))
      assert(fromToResponse.text().contains("Exactly, both is fine but do pick one."))
      assert(fromToResponse.statusCode == 200)

    }
  }
}