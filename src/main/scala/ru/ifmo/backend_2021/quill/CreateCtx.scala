package ru.ifmo.backend_2021.quill

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{LowerCase, PostgresJdbcContext}

object CreateCtx {
  def apply(): PostgresJdbcContext[LowerCase.type] = {
    val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
    pgDataSource.setUser("postgres")
    pgDataSource.setPassword("1")
    val config = new HikariConfig()
    config.setDataSource(pgDataSource)
    new PostgresJdbcContext(LowerCase, new HikariDataSource(config))
  }

}
