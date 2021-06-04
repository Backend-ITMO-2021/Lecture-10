package ru.ifmo.backend_2021
import java.util.Date

case class Message(id: String, username: String, msg: String, idParent: String = "none", time: Date) {}
