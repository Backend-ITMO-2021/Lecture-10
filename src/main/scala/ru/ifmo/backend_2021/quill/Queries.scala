package ru.ifmo.backend_2021.quill

import io.getquill.{LowerCase, Ord, PostgresJdbcContext}

object Queries {
  def topTenLanguagesSpokenByCities(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._
    ctx.run(
      query[City]
        .join(query[CountryLanguage])
        .on(_.countryCode == _.countryCode)
        .groupBy(c => c._2.language)
        .map{case (language, name) => (language, name.size)}
        .sortBy(_._2)(Ord.descNullsLast)
        .take(10)
    )
  }
  def topTenLanguagesSpokenByPopulation(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._
    ctx.run(
      query[City]
        .join(query[CountryLanguage])
        .on(_.countryCode == _.countryCode)
        .groupBy(c => c._2.language)
        .map{case (language, countryLanguage) => (language, countryLanguage.map{ case(countryLanguage, city) => city.percentage * countryLanguage.population / 100}.sum)}
        .sortBy(_._2)(Ord.descNullsLast)
        .take(10)
    ).map { case (language, population) => (language, population.getOrElse(.0).toLong) }
  }
}
