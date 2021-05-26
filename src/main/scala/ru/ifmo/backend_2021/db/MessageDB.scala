package ru.ifmo.backend_2021.db

import ru.ifmo.backend_2021.Message

trait MessageDB {
  def getMessages: List[Message]
  def addMessage(username: String, message: String, replyTo: Option[Int]): Unit
//  def addMessage(message: Message): Unit
}
