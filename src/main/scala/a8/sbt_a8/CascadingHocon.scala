package a8.sbt_a8


import java.nio.file.{Path, Paths}

import IoOps._
import HoconOps._
import com.typesafe.config.Config

/**
  * this is a copy of the class of the same name in Odin
  */
object CascadingHocon {

  def loadConfigsInDirectory(dir: Path, recurse: Boolean = true, resolve: Boolean = true): Config = {

    val normalizedPath = dir.toAbsolutePath.normalize()

    def impl = {

//      println(s"loadConfigsInDirectory(${dir}) ${dir.getCanonicalFile.getParentFile}")

      val parentConfig =
        Option(normalizedPath.getParent) match {
          case Some(parentDir) if recurse =>
            loadConfigsInDirectory(parentDir, true, false)
          case _ =>
            parseHocon("")
        }

      val filesToTry = List("developer.properties", "developer.hocon").map(dir.resolve)
      val config =
        filesToTry
          .filter(_.isFile)
          .map(HoconOps.impl.loadConfig)
          .foldLeft(parentConfig) { case (acc, c) =>
            c.withFallback(acc)
          }

      if ( resolve )
        config.resolve()
      else
        config

    }

    impl

  }


}



