package a8.sbt_a8.dobby

import java.nio.file.Paths

import a8.sbt_a8.ProjectLogger

object WebServerDemo extends App {

  implicit val logger =
    new ProjectLogger {
      override def debug(msg: String): Unit = println(s"debug | ${msg}")
      override def info(msg: String): Unit = println(s"info | ${msg}")
      override def warn(msg: String): Unit = println(s"warn | ${msg}")
    }

  val ws = new WebServer(Paths.get("."))

  ws.start

}
