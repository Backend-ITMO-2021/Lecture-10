package ru.ifmo.backend_2021.db

import ru.ifmo.backend_2021.Message

trait MessageDB {
  def getMessages: List[Message]
  def getMessages(filter: String): List[Message]
  def getUsersTop: List[(String, Long)]
  def addMessage(message: Message): Unit
}
