package ru.ifmo.backend_2021.db

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{LowerCase, PostgresJdbcContext}
import ru.ifmo.backend_2021.{AppUser, AppUserDTO, Message, MessageDTO}

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
  ctx.executeAction("CREATE TABLE IF NOT EXISTS message (id serial, replyTo integer default null, username text, message text, date timestamp not null default now());")
  ctx.executeAction("CREATE TABLE IF NOT EXISTS appUser (id serial, nickname varchar(160), userFilter varchar(160) default null, isCascade boolean default false);")


  import ctx._

  def getMessages: List[Message] =
    ctx.run(query[Message])
  def addMessage(messageDto: MessageDTO): Unit =
    ctx.run(query[Message].insert(
      _.replyTo -> lift(messageDto.replyTo),
      _.username -> lift(messageDto.username),
      _.message -> lift(messageDto.message)))

  def addUser(userDTO: AppUserDTO): Unit = {
   ctx.run(query[AppUser].insert(
     _.nickname -> lift(userDTO.nickname),
     _.userFilter -> lift(userDTO.userFilter),
     _.isCascade -> lift(userDTO.isCascade)
   ))
  }

  def getUserByNickname(nick: String): Option[AppUser] = {
    ctx.run(query[AppUser].filter(_.nickname == lift(nick))).headOption
  }

  override def updateUserFilterById(id: Int, filter: Option[String]): Unit = {
    ctx.run(query[AppUser].filter(_.id == lift(id)).update(_.userFilter -> lift(filter)))
  }
}




