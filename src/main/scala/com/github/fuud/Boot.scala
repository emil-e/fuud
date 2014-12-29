package com.github.fuud

import scala.concurrent.duration._
import akka.actor.{Props, ActorSystem}
import akka.io.IO
import akka.util.Timeout
import akka.pattern.ask
import spray.can.Http

object Boot {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("fuud")
    val service = system.actorOf(Props[FuudService], "fuud-service")
    implicit val timeout = Timeout(5.seconds)
    IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
  }
}
