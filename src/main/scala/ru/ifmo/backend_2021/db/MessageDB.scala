package ru.ifmo.backend_2021.db

import ru.ifmo.backend_2021.{AppUser, AppUserDTO, Message, MessageDTO}

trait MessageDB {
  def getMessages(filter: Option[String]): List[Message]
  def addMessage(messageDTO: MessageDTO): Unit
  def getMessagesStatsTop(): List[(String, Long)]
  def getMessagesFilteredByDate(from: Option[Long], to: Option[Long]): List[Message]

  def addUser(userDTO: AppUserDTO): Unit
  def getUserByNickname(nickname: String): Option[AppUser]

  def updateUserFilterById(id: Int, filter: Option[String]): Unit

  def getUserStatsByNickname(nickName: String): Long
}
