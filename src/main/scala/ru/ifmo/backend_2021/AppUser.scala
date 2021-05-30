package ru.ifmo.backend_2021

case class AppUser(id: Int, nickname: String, userFilter: Option[String], isCascade: Boolean)

case class AppUserDTO(nickname: String, userFilter: Option[String] = None, isCascade: Boolean = false)
