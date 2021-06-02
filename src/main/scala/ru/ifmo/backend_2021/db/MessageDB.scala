package ru.ifmo.backend_2021.db

import ru.ifmo.backend_2021.Message

trait MessageDB {
  def getMessages: List[Message]
  def getMessagesByDate(from: Option[Long], to: Option[Long]): List[Message]
  def getUserMessages(username: String): List[Message]
  def getUserStats(username: String): Long
  def getTopUsers(): List[(String, Long)]
  def addMessage(username: String, message: String, parentId: Option[Int]): Unit
}
