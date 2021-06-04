package ru.ifmo.backend_2021

import scalatags.Text
import scalatags.Text.all._

import java.sql.Timestamp


case class Message(id: Int, replyTo: Option[Int], username: String, message: String, date: String) {
  def toFile: String = s"$username#$message"
  def toListItemStr: Text.TypedTag[String] = {

    span(i(s"#${id.toString}"), " ", if(replyTo.isDefined) s"-> #${replyTo.get}" else "  ", "   ",  b(username), " ", message)
  }
}

case class MessageDTO(replyTo: Option[Int], username: String, message: String)
