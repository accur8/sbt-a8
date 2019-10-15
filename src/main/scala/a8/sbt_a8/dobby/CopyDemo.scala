package a8.sbt_a8.dobby

import java.nio.file.Paths

import a8.sbt_a8.ProjectLogger

object CopyDemo extends App {

  implicit val logger =
    new ProjectLogger {
      override def debug(msg: String): Unit = println("debug | " + msg)
      override def info(msg: String): Unit = println("info | " + msg)
      override def warn(msg: String): Unit = println("warn | " + msg)
    }

  DobbyImpl.copyDirectory(Paths.get("/Users/glen/code/accur8/odin/mugatu/client/webapp-dobby"), Paths.get("/Users/glen/code/accur8/odin/mugatu/client/webapp-composite"))

}
