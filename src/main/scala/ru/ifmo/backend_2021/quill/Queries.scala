package ru.ifmo.backend_2021.quill

import io.getquill._

object Queries {
  def topTenLanguagesSpokenByCities(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._
    ctx.run(
      query[City]
        .join(query[CountryLanguage])
        .on((lang, city) => lang.countryCode == city.countryCode)
        .groupBy(city => city._2.language)
        .map{case (lang, name) => (lang, name.size)}
        .sortBy(_._2)(Ord.descNullsLast)
        .take(10)
    )
  }
  def topTenLanguagesSpokenByPopulation(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._
    ctx.run(
      query[City]
        .join(query[CountryLanguage])
        .on((lang, city) => lang.countryCode == city.countryCode)
        .groupBy(city => city._2.language)
        .map{case (lang, countryLang) => (lang, countryLang.map{ case(countryLang, city) => city.percentage * countryLang.population / 100}.sum)}
        .sortBy(_._2)(Ord.descNullsLast)
        .take(10)
    ).map { case (lang, population) => (lang, population.getOrElse(.0).toLong) }
  }
}