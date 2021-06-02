package ru.ifmo.backend_2021.db

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{LowerCase, Ord, PostgresJdbcContext}
import ru.ifmo.backend_2021.Message

class DBAdapter() extends MessageDB {
  val server: EmbeddedPostgres = EmbeddedPostgres.builder()
    .setDataDirectory("./data")
    .setCleanDataDirectory(false)
    .setPort(5432)
    .start()
  val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
  pgDataSource.setUser("postgres")
  pgDataSource.setPassword("1")
  val hikariConfig = new HikariConfig()
  hikariConfig.setDataSource(pgDataSource)
  val ctx = new PostgresJdbcContext(LowerCase, new HikariDataSource(hikariConfig))
  ctx.executeAction("DROP TABLE IF EXISTS message CASCADE;CREATE TABLE IF NOT EXISTS message (id serial PRIMARY KEY, username text NOT NULL, message text NOT NULL, parent int, date bigint)")

  import ctx._

  def getMessages: List[Message] =
    ctx.run(query[Message])

  def addMessage(message: Message): Unit =
    ctx.run(query[Message].insert(lift(message)))

  def getMessagesByUser(user: String): List[String] =
    ctx.run(query[Message].filter(_.username == lift(user))).map(msg => msg.message)

  def getTop10Chatters: List[(String, Long)] =
    ctx.run(
      query[Message]
        .groupBy(msg => msg.username)
        .map{case (username, msg) => (username, msg.size)}
        .sortBy(_._2)(Ord.descNullsLast)
        .take(10)
    )

  def getMessagesOrderedByDate(from: Long = 0, to: Long = Long.MaxValue): List[Message] = {
    ctx.run(
      query[Message]
        .filter(msg => msg.date >= lift(from) && msg.date <= lift(to))
        .sortBy(_.date)(Ord.ascNullsLast)
    )
  }

  if (getMessages.isEmpty) {
    val data = List(
      Message(1, "test", "First message!", "".toIntOption, System.currentTimeMillis()),
      Message(2, "willy", "I'm here by chance.", "".toIntOption, System.currentTimeMillis()),
    )
    data.foreach(frame => addMessage(frame))
  }
}




