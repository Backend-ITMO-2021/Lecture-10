package ru.ifmo.backend_2021.quill

import io.getquill.{LowerCase, PostgresJdbcContext, Ord}

object Queries {
  def topTenLanguagesSpokenByCities(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._

    ctx.run(
      query[CountryLanguage]
        .join(query[City])
        .on{_.countryCode == _.countryCode}
        .groupBy{case (country, _) => country.language}
        .map{case (language, list) => (language, list.size)}
        .sortBy{case (_, size) => size}(Ord.desc)
        .take(10)
    )
  }
  def topTenLanguagesSpokenByPopulation(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._

    ctx.run(
      query[CountryLanguage]
        .join(query[City])
        .on{_.countryCode == _.countryCode}
        .groupBy{case (country, _) => country.language}
        .map{case (language, list) => (
          language,
          list.map{case (country, city) => country.percentage * city.population / 100}.sum
        )}
        .sortBy{case (_, sum) => sum}(Ord.desc)
        .take(10)
    ).map{case (language, population) => (language, population.getOrElse(.0).toLong)}
  }
}
