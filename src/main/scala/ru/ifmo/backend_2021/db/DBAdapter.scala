package ru.ifmo.backend_2021.db

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{LowerCase, PostgresJdbcContext}
import ru.ifmo.backend_2021.Message

class DBAdapter() extends MessageDB {
  val server: EmbeddedPostgres = EmbeddedPostgres.builder()
    .setDataDirectory("./data")
    .setCleanDataDirectory(true)
    .setPort(5432)
    .start()
  val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
  pgDataSource.setUser("postgres")
  pgDataSource.setPassword("123")
  val hikariConfig = new HikariConfig()
  hikariConfig.setDataSource(pgDataSource)
  val ctx = new PostgresJdbcContext(LowerCase, new HikariDataSource(hikariConfig))
  ctx.executeAction("DROP TABLE IF EXISTS message CASCADE;CREATE TABLE IF NOT EXISTS message (id serial PRIMARY KEY, username text NOT NULL, message text NOT NULL, replyTo text, date text);")

  import ctx._

  def getMessages: List[Message] =
    ctx.run(query[Message])
  def addMessage(message: Message): Unit =
    ctx.run(query[Message].insert(lift(message)))

}




