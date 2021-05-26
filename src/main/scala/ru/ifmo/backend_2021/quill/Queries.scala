package ru.ifmo.backend_2021.quill

import io.getquill._

object Queries {
  def topTenLanguagesSpokenByCities(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._

    val q = quote {
      query[CountryLanguage]
        .join(query[City])
        .on{(l, c) => l.countryCode == c.countryCode}
        .groupBy{case (l, c) => l.language}
        .map{case (language, lc) => (language, lc.size)}
        .sortBy{case (language, size) => size}(Ord.descNullsLast)
        .take(10)
    }

    ctx.run(q)
  }

  def topTenLanguagesSpokenByPopulation(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = ???
}
