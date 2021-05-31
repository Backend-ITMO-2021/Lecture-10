package ru.ifmo.backend_2021.db

import ru.ifmo.backend_2021.Message

import java.util.Date

trait MessageDB {
  def getMessages: List[Message]
  def addMessage(message: Message): Either[String, Message]

  def getUserMessages(username: String): List[Message]
  def getUserMessagesStats(username: String): Long
  def getUserMessagesTop: List[(String, Long)]

  def getMessagesByDate(from: Option[Date], to: Option[Date]): List[Message]
}
