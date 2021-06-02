package ru.ifmo.backend_2021

import java.util.Date

case class Message(
    id: Int,
    username: String,
    message: String,
    parentId: Option[Int],
    date: Date
) {}
