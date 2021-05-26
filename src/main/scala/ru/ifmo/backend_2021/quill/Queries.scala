package ru.ifmo.backend_2021.quill

import io.getquill.{LowerCase, PostgresJdbcContext, Ord}

object Queries {
  def topTenLanguagesSpokenByCities(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._

    ctx.run(
      query[CountryLanguage]
        .join(query[City])
        .on{(l, c) => l.countryCode == c.countryCode}
        .groupBy{case (l, c) => l.language}
        .map{case (language, lc) => (language, lc.size)}
        .sortBy{case (language, size) => size}(Ord.descNullsLast)
        .take(10)
    )
  }

  def topTenLanguagesSpokenByPopulation(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._

    ctx.run(
      query[CountryLanguage]
        .join(query[City])
        .on{(l, c) => l.countryCode == c.countryCode}
        .groupBy{case (l, c) => l.language}
        .map{case (language, lc) => (
            language,
            lc.map {case (l, c) => l.percentage * c.population}.sum
//            lc.map {case (l, c) => l.percentage * c.population / 100}.sum
          )
        }
        .sortBy{case (language, population) => population}(Ord.descNullsLast)
        .take(10)
    )
      .map{case (language, population) => (language, population.map(_.toLong).getOrElse(0))}
  }
}
