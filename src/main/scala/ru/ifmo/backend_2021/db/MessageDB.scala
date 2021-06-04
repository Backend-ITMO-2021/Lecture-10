package ru.ifmo.backend_2021.db

import ru.ifmo.backend_2021.Message

trait MessageDB {
  def getMessages(filter: Option[String] = None): List[Message]
  def addMessage(id: String, username: String, msg: String, idParent: String): Unit
  def getTop10Chatters: List[(String, Long)]
  def getMessagesByDate(from: Option[Long], to: Option[Long]): List[Message]
  def getUserStats(username: String): Long
}
