package ru.ifmo.backend_2021.quill

import io.getquill._

object Queries {
  def topTenLanguagesSpokenByCities(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._

    val statisticsQuery = quote {
      query[Country]
        .join(query[CountryLanguage])
        .on(_.code == _.countryCode)
        .join(query[City])
        .on(_._1.code == _.countryCode)
        .groupBy{ case (tuple, _) => tuple._2.language}
        .map{ case (language, q) => (language, q.size) }
        .sortBy{ case (_, l) => l}(Ord.descNullsLast)
        .take(10)
    }

    ctx.run(statisticsQuery)
  }
  def topTenLanguagesSpokenByPopulation(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._

    val statisticsQuery2 = quote {
            query[Country]
              .join(query[CountryLanguage])
              .on(_.code == _.countryCode)
              .join(query[City])
              .on(_._1.code == _.countryCode)
              .map{ case (tuple, city) => (tuple._2, city)}
              .groupBy({ case (language, _) => language.language})
              .map{ case (lang, clang) => (lang, clang.map({ case (language, city) =>
                language.percentage * city.population / 100})
                .sum)}
              .sortBy{ case (_, l) => l}(Ord.descNullsLast)
              .map{ case (lang, maybeLong) => (lang, maybeLong.getOrElse(0d))}
              .take(10)
          }
    val res = ctx.run(statisticsQuery2)
    res.map{ case (str, d) => (str, d.toLong)}
  }
}
