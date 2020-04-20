package a8.sbt_a8

import java.nio.charset.StandardCharsets._
import java.nio.file.attribute.FileAttribute
import java.nio.file.{Files, Paths}

/**
  * this is a copy of the class of the same name in Odin
  */
object IoOps {

  def readFile(file: java.io.File): String = {
    new String(Files.readAllBytes(Paths.get(file.getCanonicalPath)), UTF_8)
  }

  def fileExtension(file: java.io.File): String = {
    val name = file.getName
    name.lastIndexOf(".") match {
      case i if i >= 0 && i < name.length =>
        file.getName.substring(i+1)
      case _ =>
        ""
    }
  }

  implicit class PathOps(path: java.nio.file.Path) {

    def exists = Files.exists(path)

    def isFile = Files.isRegularFile(path)
    def isDirectory = Files.isDirectory(path)

    def parentOpt = Option(path.getParent)

  }

}
