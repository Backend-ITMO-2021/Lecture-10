package ru.ifmo.backend_2021.db

import ru.ifmo.backend_2021.Message

trait MessageDB {
  def getMessages: List[Message]
  def addMessage(message: Message): Unit
  def getMessagesByUser(user: String): List[String]
  def getTop10Chatters: List[(String, Long)]
  def getMessagesOrderedByDate(from: Long = 0, to: Long = Long.MaxValue): List[Message]
}
