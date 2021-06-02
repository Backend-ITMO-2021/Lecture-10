package ru.ifmo.backend_2021.db

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{LowerCase, PostgresJdbcContext, Ord}
import ru.ifmo.backend_2021.Message

import java.util.Date

class DBAdapter() extends MessageDB {
  val server: EmbeddedPostgres = EmbeddedPostgres
    .builder()
    .setDataDirectory("./data")
    .setCleanDataDirectory(false)
    .setPort(5432)
    .start()
  val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
  pgDataSource.setUser("postgres")
  val hikariConfig = new HikariConfig()
  hikariConfig.setDataSource(pgDataSource)
  val ctx =
    new PostgresJdbcContext(LowerCase, new HikariDataSource(hikariConfig))
  ctx.executeAction(
    "CREATE TABLE IF NOT EXISTS message (id serial, username text, message text, parentId int default null, date timestamp not null default now());"
  )
  ctx.executeAction(
    "TRUNCATE TABLE message RESTART IDENTITY CASCADE;;"
  )

  lazy val defaultMessages =
    List(
      (
        "ventus976",
        "I don't particularly care which interaction they pick so long as it's consistent.",
        None
      ),
      (
        "XimbalaHu3",
        "Exactly, both is fine but do pick one.",
        None
      ),
      (
        "XimbalaHu3",
        "Worse it!",
        Option(2)
      ),
      (
        "thresh",
        "Test Message!",
        None
      ),
      (
        "thresh",
        "Test Message!",
        None
      ),
      (
        "thresh",
        "You can always trust Braum!",
        None
      )
    )

  defaultMessages.map(m => addMessage(m._1, m._2, m._3))

  import ctx._

  def getMessages: List[Message] =
    ctx.run(query[Message])

  def getMessagesByDate(from: Option[Long], to: Option[Long]): List[Message] = {
    implicit class DateQuotes(left: Date) {
      def >(right: Date) = quote(infix"$left > $right".as[Boolean])

      def <(right: Date) = quote(infix"$left < $right".as[Boolean])
    }

    if (List(from, to).exists(_.nonEmpty))
      ctx.run(
        query[Message]
          .filter(_.date > lift(from.map(new Date(_)).getOrElse(new Date(0))))
          .filter(_.date < lift(to.map(new Date(_)).getOrElse(new Date())))
      )
    else getMessages
  }

  def getUserMessages(username: String): List[Message] =
    ctx.run(query[Message].filter(_.username == lift(username)))

  def getUserStats(username: String): Long =
    ctx.run(query[Message].filter(_.username == lift(username)).size)

  def getTopUsers(): List[(String, Long)] =
    ctx.run(
      query[Message]
        .groupBy(_.username)
        .map { case (u, s) => (u, s.size) }
        .sortBy { case (_, s) => s }(Ord.desc)
        .take(10)
    )

  def addMessage(
      username: String,
      message: String,
      parentId: Option[Int]
  ): Unit = {
    ctx.run(
      query[Message].insert(
        _.username -> lift(username),
        _.message -> lift(message),
        _.parentId -> lift(parentId)
      )
    )
  }

}
