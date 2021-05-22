package ru.ifmo.backend_2021.quill

import io.getquill.{LowerCase, PostgresJdbcContext}
import ru.ifmo.backend_2021.utils.FinallyClose

import scala.io.Source


object Setup extends App {
  val ctx: PostgresJdbcContext[LowerCase.type] = CreateCtx()

  FinallyClose(Source.fromURL(getClass.getResource("/world.sql")))(source =>
    ctx.executeAction(source.toList.mkString(""))
  )
}
