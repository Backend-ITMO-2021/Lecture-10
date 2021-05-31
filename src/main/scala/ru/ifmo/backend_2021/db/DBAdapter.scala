package ru.ifmo.backend_2021.db

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{LowerCase, Ord, PostgresJdbcContext}
import ru.ifmo.backend_2021.Message

import java.util.Date

class DBAdapter(clear: Boolean) extends MessageDB {
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
  ctx.executeAction("CREATE TABLE IF NOT EXISTS message (id serial PRIMARY KEY, username text NOT NULL, message text NOT NULL, replyTo int REFERENCES message (id), sent timestamp);")

  if (clear) {
    ctx.executeAction("TRUNCATE TABLE message;")
    ctx.executeAction("ALTER SEQUENCE message_id_seq RESTART WITH 1;")
    addMessage(Message(None, "ventus976", "I don't particularly care which interaction they pick so long as it's consistent.", None, new Date()))
    addMessage(Message(None, "XimbalaHu3", "Exactly, both is fine but do pick one.", None, new Date()))
  }

  import ctx._

  def getMessages: List[Message] =
    ctx.run(query[Message])

  def addMessage(message: Message): Either[String, Message] = {
    if (message.replyTo.nonEmpty && ctx.run(query[Message].filter(_.id == lift(message.replyTo)).isEmpty)) {
      return Left("replyTo")
    }

    val id = ctx.run(query[Message].insert(lift(message)).returningGenerated(_.id)).get
    Right(message.id(id))
  }

  def getUserMessages(username: String): List[Message] =
    ctx.run(query[Message].filter(_.username == lift(username)))

  def getUserMessagesStats(username: String): Long =
    ctx.run(query[Message].filter(_.username == lift(username)).size)

  def getUserMessagesTop: List[(String, Long)] =
    ctx.run(query[Message].
      groupBy(_.username).map { case (username, ms) => (username, ms.size) }
      .sortBy(_._2)(Ord.desc)
      .take(10))

  def getMessagesByDate(from: Option[Date], to: Option[Date]): List[Message] = {

    //noinspection TypeAnnotation
    implicit class DateQuotes(left: Date) {

      def >=(right: Date) = quote(infix"$left >= $right".as[Boolean])
      def <=(right: Date) = quote(infix"$left <= $right".as[Boolean])
    }

    ctx.run(dynamicQuery[Message]
      .filterOpt(from)((m, d) => quote(m.sent >= d))
      .filterOpt(to)((m, d) => quote(m.sent <= d)))
  }
}
