package ru.ifmo.backend_2021.db

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{LowerCase, PostgresJdbcContext}
import ru.ifmo.backend_2021.Message

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
  ctx.executeAction("CREATE TABLE IF NOT EXISTS message (" +
    "id serial, " +
    "time timestamp, " +
    "username text, " +
    "message text, " +
    "replyTo integer);")

  import ctx._

  def getMessages: List[Message] =
    ctx.run(query[Message])
  def getMessages(filter: String): List[Message] =
    if (filter == "") getMessages else ctx.run(query[Message].filter(_.username == lift(filter)).map(msg => Message(msg.id, msg.time, msg.username, msg.message, None)))
  def addMessage(message: Message): Unit = {
    ctx.run(query[Message].insert(lift(message)))
  }
}
