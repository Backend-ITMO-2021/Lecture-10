package ru.ifmo.backend_2021

import java.util.Date

case class Message(id: Option[Int], username: String, message: String, replyTo: Option[Int], sent: Date) {

  def id(id: Int): Message = Message(Some(id), username, message, replyTo, sent)

  def toFile: String = s"${id.get}#$username#$message#${replyTo.getOrElse(-1)}#${sent.getTime}"
}

object Message {

  def apply(fromString: String): Message = {
    val List(id, username, message, replyTo, sent) = fromString.split("#").toList

    Message(Some(id.toInt), username, message, Some(replyTo.toInt).filter(_ > 0), new Date(sent.toLong))
  }
}
