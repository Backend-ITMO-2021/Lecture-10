package ru.ifmo.backend_2021.db

import ru.ifmo.backend_2021.Message

trait MessageDB {
  def getMessages: List[Message]
  def addMessage(message: Message): Unit
  def getUserMessages(user: String): List[Message]
  def getTopUsers: List[(String, Long)]
  def dateFilterMessages(from: Long, to: Long): List[Message]
  def getCountMessages(user: String): Int
}
