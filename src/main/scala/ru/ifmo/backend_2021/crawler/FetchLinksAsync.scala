package ru.ifmo.backend_2021.crawler

import org.asynchttpclient.{AsyncHttpClient, Dsl, ListenableFuture, Response}

import scala.concurrent.{ExecutionContext, Future, Promise}

object FetchLinksAsync {
  val asyncHttpClient: AsyncHttpClient = Dsl.asyncHttpClient()

  def fetchLinksAsync(title: String)(implicit ec: ExecutionContext): Future[Seq[String]] = {
    val p = Promise[String]
    val listenableFuture: ListenableFuture[Response] =
      asyncHttpClient.prepareGet("https://en.wikipedia.org/w/api.php")
        .addQueryParam("action", "query")
        .addQueryParam("titles", title)
        .addQueryParam("prop", "links")
        .addQueryParam("format", "json")
        .execute()

    listenableFuture.addListener(() => p.success(listenableFuture.get().getResponseBody), null)
    val scalaFuture: Future[String] = p.future
    scalaFuture.map { responseBody =>
      for {
        page <- ujson.read(responseBody)("query")("pages").obj.values.toSeq
        links <- page.obj.get("links").toSeq
        link <- links.arr
      } yield link("title").str
    }
  }
}
