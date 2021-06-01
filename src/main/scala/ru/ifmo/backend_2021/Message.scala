package ru.ifmo.backend_2021

case class Message(
    id: Int,
    username: String,
    message: String,
    parentId: Option[Int]
) {}
