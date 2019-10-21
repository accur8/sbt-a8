package a8.sbt_a8.dobby


import java.nio.file.{Files, LinkOption, Path, Paths, StandardCopyOption}
import java.util.jar.JarFile

import a8.sbt_a8.ProjectLogger
import sbt.internal.util.Attributed
import a8.sbt_a8.Utilities._
import sbt._
import Keys._

import scala.collection.JavaConverters._
import scala.collection.mutable

object DobbyImpl {

  lazy val webappCompositeName = "webapp-composite"
  lazy val webappDobbyName = "webapp-dobby"


  def runStart(packageFile: java.io.File, projectRoot: java.io.File, jars1: Iterable[Attributed[java.io.File]], httpPort: Int, settings: DobbySettings)(implicit logger: ProjectLogger): Unit = {
    runSetup(packageFile, projectRoot, jars1, false)
    stopServer(httpPort, settings)
    val webappCompositeDir = projectRoot / webappCompositeName
    val ws = new WebServer(webappCompositeDir.toPath)
    ws.start
    settings.dobbyActiveServers(httpPort) = ws
  }

  def runSetup(packageFile: java.io.File, projectRoot: java.io.File, jars0: Iterable[Attributed[java.io.File]], force: Boolean)(implicit logger: ProjectLogger): Unit = {

    val jars: Iterable[java.io.File] = jars0.map(_.data).toList.distinct

    import java.util.jar._

    import scala.collection.JavaConverters._

    val webappCompositeDir = projectRoot / webappCompositeName

    if ( force ) {
      sbt.IO.delete(webappCompositeDir)
    }

    if ( webappCompositeDir.exists ) {
      logger.debug(s"${webappCompositeDir} already exists no action taken")
    } else {

      webappCompositeDir.mkdirs()

      explodeWebjars(jars, webappCompositeDir)

      // copy over the webapp-extras
      val webappExtras = projectRoot / webappDobbyName
      if ( webappExtras.exists() ) {
        copyDirectory(webappExtras.toPath, webappCompositeDir.toPath)
      }

    }

  }

  def copyDirectory(source: Path, target: Path)(implicit logger: ProjectLogger): Unit = {
    logger.debug(s"copyDirectory ${source} ${target}")
    if ( Files.isDirectory(source) ) {
      if ( !Files.isDirectory(target) ) {
        Files.createDirectories(target)
      }
      Files
        .list(source)
        .forEach { entry: Path =>
          val entryTarget = target.resolve(entry.getFileName)
          if ( Files.isDirectory(entry) ) {
            copyDirectory(entry, entryTarget)
          } else {
            copyFile(entry, entryTarget)
          }
        }
    } else {
      logger.warn(s"unable to copy directory ${source} ${target}")
    }
  }

  def copyFile(source: Path, target: Path)(implicit logger: ProjectLogger): Unit = {
    if ( Files.isSymbolicLink(source) ) {
      val canonicalSource = source.toFile.getCanonicalFile.toPath
      Files.deleteIfExists(target)
      if ( Files.exists(source) ) {
        logger.debug(s"canonicalized createSymbolicLink ${target} ${canonicalSource}")
        Files.createSymbolicLink(target, canonicalSource)
      } else {
          val f = Files.readSymbolicLink(source)
        logger.debug(s"relative createSymbolicLink ${target} ${canonicalSource}")
        Files.createSymbolicLink(target, f)
      }
    } else if ( Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS) ) {
      logger.debug(s"copyFile ${source} ${target}")
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    } else {
      logger.warn(s"this should never happen unable to copy ${source} ${target}")
    }

  }

  private def explodeWebjars(jars: Iterable[File], webappCompositeDir: File)(implicit logger: ProjectLogger) = {
    val prefixes = List("webapp/")
    for (artifact <- jars) {
      logger.info("processing artifact " + artifact)
      if (artifact.isFile && artifact.getName.endsWith("jar")) {
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
      } else if ( artifact.isDirectory ) {
        val webappDir = new File(artifact, "webapp")
        symlinkDir(webappDir, webappCompositeDir)
      } else {
        logger.warn(s"explodeWebjars() don't know how to handle artifact ${artifact.getAbsolutePath}")
      }
    }
  }

  def symlinkDir(sourceDir: File, targetDir: File)(implicit logger: ProjectLogger): Unit = {
    if ( !targetDir.exists() )
      targetDir.mkdirs()
    val entries = Option(sourceDir.listFiles()).getOrElse(Array.empty)
    entries
      .foreach { sourceEntry =>
        val targetEntry = new File(targetDir, sourceEntry.name)
        sourceEntry match {
          case f if f.isDirectory =>
            symlinkDir(f, targetEntry)
          case _ if targetEntry.exists() =>
            logger.warn(s"symlinkDir() targetEntry ${targetEntry}  already exists unable to link")
          case f if f.isFile =>
            val link =
            Files.createSymbolicLink(targetEntry.getAbsoluteFile.asPath, sourceEntry.getAbsoluteFile.asPath)
          case f =>
            logger.warn(s"symlinkDir() don't know how to handle ${f.getAbsolutePath}")
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
