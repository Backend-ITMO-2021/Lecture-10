package ru.ifmo.backend_2021.crawler

import ru.ifmo.backend_2021.crawler.FetchLinksAsync.{asyncHttpClient, fetchLinksAsync}

import java.util.concurrent.Executors
import scala.concurrent.duration.Duration.Inf
import scala.concurrent.{Await, ExecutionContext, Future}

object AsyncCrawler extends App {
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  def fetchAllLinksAsync(startTitle: String, depth: Int): Future[Set[String]] = {
    def rec(current: Set[String], seen: Set[String], recDepth: Int): Future[Set[String]] = {
      if (recDepth >= depth) Future.successful(seen)
      else {
        val futures = for (title <- current) yield fetchLinksAsync(title)
        Future.sequence(futures).map { nextTitleLists =>
          val nextTitles = nextTitleLists.flatten
          rec(nextTitles.filter(!seen.contains(_)), seen ++ nextTitles, recDepth + 1)
        }.flatten
      }
    }

    rec(Set(startTitle), Set(startTitle), 0)
  }
  TimeColored {
    pprint.pprintln(Await.result(fetchAllLinksAsync("Albert Einstein", 4), Inf))
  }
  ec.shutdown()
  asyncHttpClient.close()
}
