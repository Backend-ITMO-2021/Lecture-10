package ru.ifmo.backend_2021.quill

import io.getquill.{LowerCase, PostgresJdbcContext}

object Queries {
  def topTenLanguagesSpokenByCities(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = ???
  def topTenLanguagesSpokenByPopulation(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = ???
}
