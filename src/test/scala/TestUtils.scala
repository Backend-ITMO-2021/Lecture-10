import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{LowerCase, PostgresJdbcContext}
import ru.ifmo.backend_2021.utils.FinallyClose
import ujson.{Readable, Value}

import java.io.File
import scala.io.Source
import scala.reflect.io.Directory
import scala.util.Try

object TestUtils {
  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = io.undertow.Undertow.builder
      .addHttpListener(8081, "localhost")
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f("http://localhost:8081")
      finally server.stop()
    res
  }

  def withDB[T](f: PostgresJdbcContext[LowerCase.type] => T): T = {
    val server = EmbeddedPostgres.builder()
      .setDataDirectory("./test-data")
      .setCleanDataDirectory(true)
      .setPort(1521)
      .start()
    val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
    pgDataSource.setUser("postgres")
    pgDataSource.setPortNumber(1521)
    val config = new HikariConfig()
    config.setDataSource(pgDataSource)
    val ctx: PostgresJdbcContext[LowerCase.type] = new PostgresJdbcContext(LowerCase, new HikariDataSource(config))
    FinallyClose(Source.fromURL(getClass.getResource("/world.sql")))(source =>
      ctx.executeAction(source.toList.mkString(""))
    )
    val result = Try {
      f(ctx)
    }
    server.close()
    val directory = new Directory(new File("./test-data"))
    directory.deleteRecursively()
    result.get
  }

  def readJsonAndAssert(r: Readable)(f: Value.Value => Boolean): Unit =
    assert(f(ujson.read(r)))
}
