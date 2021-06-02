package ru.ifmo.backend_2021.quill

import io.getquill.{LowerCase, PostgresJdbcContext, Ord}

object Queries {
  def topTenLanguagesSpokenByCities(
      ctx: PostgresJdbcContext[LowerCase.type]
  ): List[(String, Long)] = {
    import ctx._

    ctx.run(
      query[CountryLanguage]
        .join(query[City])
        .on(_.countryCode == _.countryCode)
        .groupBy { case (l, _) => l.language }
        .map { case (l, c) => (l, c.size) }
        .sortBy { case (_, s) => s }(Ord.desc)
        .take(10)
    )
  }

  def topTenLanguagesSpokenByPopulation(
      ctx: PostgresJdbcContext[LowerCase.type]
  ): List[(String, Long)] = {
    import ctx._

    ctx
      .run(
        query[CountryLanguage]
          .join(query[City])
          .on(_.countryCode == _.countryCode)
          .groupBy { case (l, _) => l.language }
          .map { case (l, c) =>
            (l, c.map { case (l, c) => l.percentage * c.population / 100 }.sum)
          }
          .sortBy { case (_, s) => s }(Ord.desc)
          .take(10)
      )
      .map { case (l, s) => (l, s.map(_.toLong).getOrElse(0)) }
  }
}
