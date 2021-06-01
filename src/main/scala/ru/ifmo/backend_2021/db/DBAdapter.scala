package ru.ifmo.backend_2021.db

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{LowerCase, PostgresJdbcContext}
import ru.ifmo.backend_2021.Message

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
    "CREATE TABLE IF NOT EXISTS message (id serial, username text, message text, parentId int, timestamp timestamp not null default now());"
  )

  import ctx._

  def getMessages: List[Message] =
    ctx.run(query[Message])

  def addMessage(message: Message): Unit =
    ctx.run(
      query[Message].insert(
        _.username -> lift(message.username),
        _.message -> lift(message.message),
        _.parentId -> lift(message.parentId)
      )
    )
}
