package ru.ifmo.backend_2021.quill

import io.getquill.{LowerCase, Ord, PostgresJdbcContext}

object Queries {

  def topTenLanguagesSpokenByCities(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._

    ctx.run(query[CountryLanguage]
      .join(query[City]).on(_.countryCode == _.countryCode)
      .groupBy(_._1.language).map { case (language, q) => (language, q.size) }
      .sortBy(_._2)(Ord.desc)
      .take(10))
  }

  def topTenLanguagesSpokenByPopulation(ctx: PostgresJdbcContext[LowerCase.type]): List[(String, Long)] = {
    import ctx._

    ctx.run(query[CountryLanguage]
      .join(query[City]).on(_.countryCode == _.countryCode)
      .map { case (language, city) => (language.language, language.percentage * city.population / 100) }
      .groupBy(_._1).map { case (language, q) => (language, q.map(_._2).sum) }
      .sortBy(_._2)(Ord.desc)
      .take(10))
      .map { case (language, population) => (language, population.getOrElse(.0).toLong) }
  }
}
