package ru.ifmo.backend_2021.connections

import cask.WsChannelActor
import cask.endpoints.WsHandler
import cask.util.Logger
import cask.util.Ws.Event
import ru.ifmo.backend_2021.AppUser



trait ConnectionPool {
  def send(event: Event): WsChannelActor => Unit
  def sendAll(eventActor: WsChannelActor => Event): Unit
  def wsHandler(onConnect: WsChannelActor => Unit)(nickName: String)(implicit ac: castor.Context, log: Logger): WsHandler
  def getConnectionUser(channelActor: WsChannelActor): Option[AppUser]
}
