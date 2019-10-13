package a8.sbt_a8.dobby


import java.nio.file.{Path, Paths}

import a8.sbt_a8.ProjectLogger
import sbt.internal.util.Attributed
import a8.sbt_a8.Utilities._
import sbt._
import Keys._
import scala.collection.JavaConverters._

import scala.collection.mutable

object DobbyImpl {

  def runStart(projectRoot: java.io.File, jars1: Iterable[Attributed[java.io.File]], jars2: Iterable[Attributed[java.io.File]], httpPort: Int, settings: DobbySettings)(implicit logger: ProjectLogger): Unit = {
    runSetup(projectRoot, jars1, jars2, false)
    stopServer(httpPort, settings)
    val webappCompositeDir = projectRoot / "webapp-composite"
    val ws = new WebServer(webappCompositeDir.toPath)
    ws.start
    settings.dobbyActiveServers(httpPort) = ws
  }

  def runSetup(projectRoot: java.io.File, jars1: Iterable[Attributed[java.io.File]], jars2: Iterable[Attributed[java.io.File]], force: Boolean)(implicit logger: ProjectLogger): Unit = {

    val jars: Iterable[java.io.File] = (jars1 ++ jars2).map(_.data).toList.distinct

    import java.util.jar._

    import scala.collection.JavaConverters._

    val webappCompositeDir = projectRoot / "webapp-composite"

    if ( force ) {
      sbt.IO.delete(webappCompositeDir)
    }

    if ( webappCompositeDir.exists ) {
      logger.debug(s"${webappCompositeDir} already exists no action taken")
    } else {
      webappCompositeDir.mkdirs()
      val prefixes = List("webapp/")
      for (artifact <- jars) {
        logger.info("processing artifact " + artifact)
        if (artifact.getName.endsWith("jar")){
          val jarFile = new JarFile(artifact)
          jarFile.entries.asScala.foreach { entry =>
            prefixes.find(prefix => entry.getName.startsWith(prefix)).foreach { prefix =>
              if (!entry.isDirectory && prefixes.exists(prefix => entry.getName.startsWith(prefix))) {
                logger.debug(entry.getName)
                val file = webappCompositeDir / entry.getName.replace(prefix, "")
                if (!file.getParentFile.exists)
                  file.getParentFile.mkdirs()
                file.write(jarFile.getInputStream(entry).toByteArray)
              }
            }
          }
        }
      }
    }
  }

  def stopServer(port: Int, settings: DobbySettings)(implicit logger: ProjectLogger): Unit = {

    settings
      .dobbyActiveServers
      .remove(port)
      .foreach { server =>
        server.stop()
      }

  }

}
