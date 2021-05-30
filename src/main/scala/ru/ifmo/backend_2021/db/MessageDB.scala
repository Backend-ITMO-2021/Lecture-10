package ru.ifmo.backend_2021.db

import ru.ifmo.backend_2021.{AppUser, AppUserDTO, Message, MessageDTO}

trait MessageDB {
  def getMessages: List[Message]
  def addMessage(messageDTO: MessageDTO): Unit

  def addUser(userDTO: AppUserDTO): Unit
  def getUserByNickname(nickname: String): Option[AppUser]

  def updateUserFilterById(id: Int, filter: Option[String]): Unit
}
