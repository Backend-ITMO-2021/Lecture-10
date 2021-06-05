package ru.ifmo.backend_2021

import java.util.Date

case class Message(id: Int, time: Date, username: String, message: String, replyTo: Option[Int])
