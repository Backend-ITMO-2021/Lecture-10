package ru.ifmo.backend_2021.db

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{LowerCase, PostgresJdbcContext, Ord}
import ru.ifmo.backend_2021.Message
import java.util.Date

class DBAdapter() extends MessageDB {
  val server: EmbeddedPostgres = EmbeddedPostgres.builder()
    .setDataDirectory("./data")
    .setCleanDataDirectory(false)
    .setPort(5432)
    .start()
  val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
  pgDataSource.setUser("postgres")
  val hikariConfig = new HikariConfig()
  hikariConfig.setDataSource(pgDataSource)
  val ctx = new PostgresJdbcContext(LowerCase, new HikariDataSource(hikariConfig))
  ctx.executeAction("CREATE TABLE IF NOT EXISTS message (id text, username text, msg text, idParent text, time timestamp not null default now());")

  ctx.executeAction(
    "TRUNCATE TABLE message RESTART IDENTITY CASCADE;;"
  )

  addMessage("1",
        "ventus976",
        "I don't particularly care which interaction they pick so long as it's consistent.",
        "none")
  addMessage("2",
        "XimbalaHu3",
        "Exactly, both is fine but do pick one.",
        "none")

  import ctx._

  def getMessages(filter: Option[String] = None): List[Message] =
    if (filter.isDefined) {
        ctx.run(query[Message].filter(_.username == lift(filter.get)))
      } else { ctx.run(query[Message]) }

  def addMessage(id: String, username: String, msg: String, idParent: String): Unit =
    ctx.run(query[Message].insert(_.id -> lift(id), _.username -> lift(username), _.msg -> lift(msg), _.idParent -> lift(idParent)))

  def getTop10Chatters: List[(String, Long)] =
    ctx.run(
      query[Message]
        .groupBy(msg => msg.username)
        .map{case (username, msg) => (username, msg.size)}
        .sortBy(_._2)(Ord.descNullsLast)
        .take(10)
    )

  def getMessagesByDate(from: Option[Long], to: Option[Long]): List[Message] = {
    implicit class DateQuotes(left: Date) {
      def >(right: Date) = quote(infix"$left > $right".as[Boolean])

      def <(right: Date) = quote(infix"$left < $right".as[Boolean])
    }

    if (List(from, to).exists(_.nonEmpty))
      ctx.run(
        query[Message]
          .filter(_.time > lift(from.map(new Date(_)).getOrElse(new Date(0))))
          .filter(_.time < lift(to.map(new Date(_)).getOrElse(new Date())))
      )
    else getMessages()
  }

  def getUserStats(username: String): Long =
    ctx.run(query[Message].filter(_.username == lift(username)).size)
}

  



