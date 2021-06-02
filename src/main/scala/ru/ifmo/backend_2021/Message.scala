package ru.ifmo.backend_2021

case class Message(id: Int, username: String, message: String, parent: Option[Int], date: Long) {
  def toFile: String = s"$username#$message#${parent.getOrElse(0)}#${date.toString}"
}

object Message {
  def apply(fromString: String): Message = {
    val List(id, username, message, parent, date) = fromString.split("#").toList
    Message(id.toInt, username, message, Option.unless(parent == "0")(parent.toInt), date.toLong)
  }
}
