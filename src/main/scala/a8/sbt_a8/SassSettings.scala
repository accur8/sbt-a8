package a8.sbt_a8

import java.io.File

import a8.sbt_a8.Utilities._
import sbt.Keys._
import sbt._


trait SassSettings { self: SharedSettings =>

  lazy val sassSrcJarPath = "sass-src"
  lazy val sassCompile = taskKey[Unit]("Compile Sass Code")
  lazy val sassDeps = taskKey[Unit]("Generate src-deps folder")
  lazy val sassDepsUnforced = taskKey[Unit]("Generate src-deps folder if it doesn't already exist")

  def sassProject(name: String, dir: java.io.File, id: String) =
    bareProject(dir.name, dir, id)
      .settings(settings: _*)
      .settings(sassSettings: _*)
      .settings(
        Keys.name := name,
      )

  def processSassDeps(projectRoot: java.io.File, jars0: Iterable[Attributed[java.io.File]], force: Boolean)(implicit logger: ProjectLogger): Unit = {

    val jars: Iterable[java.io.File] = jars0.map(_.data).toList.distinct

    import java.util.jar._

    import scala.collection.JavaConverters._

    val srcDepsDir = projectRoot / "target/src-deps"

    if ( force ) {
      sbt.IO.delete(srcDepsDir)
    }

    if ( srcDepsDir.exists ) {
      logger.info(s"${srcDepsDir} already exists no action taken")
    } else {
      srcDepsDir.mkdirs()
      val prefixes = List(sassSrcJarPath + "/", "webapp/")
      for (artifact <- jars) {
        logger.info("processing artifact " + artifact)
        if (artifact.getName.endsWith("jar")){
          val jarFile = new JarFile(artifact)
          jarFile.entries.asScala.foreach { entry =>
            prefixes.find(prefix => entry.getName.startsWith(prefix)).foreach { prefix =>
              if (!entry.isDirectory && prefixes.exists(prefix => entry.getName.startsWith(prefix))) {
                logger.debug(entry.getName)
                val file = srcDepsDir / entry.getName.replace(prefix, "")
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

  def runSassBuild(context: String, buildFile: String, projectRoot: java.io.File, copySources: Boolean = false)(implicit logger: ProjectLogger): Unit = {
    if ( (projectRoot / buildFile).exists ) {
      val buildProps = loadProperties(new File(projectRoot, buildFile))
      val inputFileOpt = buildProps.get("inputFile")
      val outputFileOpt = buildProps.get("outputFile")
      val options = buildProps.get("options").toList.flatMap(_.splitList(" "))

      (inputFileOpt, outputFileOpt) match {
        case (Some(inputFile), Some(outputFile)) =>
          val args = List("sass") ++ options ++ List(s"${inputFile}:${outputFile}")
          val results =
            a8.sbt_a8
              .Exec(args)(logger)
              .inDirectory(projectRoot)
              .execCaptureOutput()
          val resultLines =
            (results.stdout.linesIterator ++ results.stderr.linesIterator).filter(_.trim.nonEmpty) match {
              case i if i.isEmpty => ""
              case i =>
                "\n" + i.mkString("\n")
            }
          val message = s"${context} in ${projectRoot}${resultLines}"
          if ( results.stderr.nonEmpty )
            logger.warn(message)
          else
            logger.info(message)

          if ( copySources ) {

            val sourcesFrom = projectRoot / s"src"
            val sourcesTo = projectRoot / s"target/scala-${ScalaVersion.majorVersion}/classes/${sassSrcJarPath}"

            logger.info(s"copying sass sources from ${sourcesFrom} into ${sourcesTo}")
            sourcesTo.mkdirs

            sbt.IO.copyDirectory(sourcesFrom, sourcesTo, overwrite = true, preserveLastModified = false)

          }
        case (None, None) =>
          logger.warn(s"${context} in ${projectRoot} -- missing properties in ${buildFile}: inputFile, outputFile")
        case (None, Some(_)) =>
          logger.warn(s"${context} in ${projectRoot} -- missing property in ${buildFile}: inputFile")
        case (Some(_), None) =>
          logger.warn(s"${context} in ${projectRoot} -- missing property in ${buildFile}: outputFile")
      }
    } else {
      logger.info(s"${context} in ${projectRoot} -- build file ${buildFile} not found")
    }
  }

  def sassSettings: Seq[Def.Setting[_]] =
    Seq(
        sassDeps := processSassDeps(baseDirectory.value, (Compile / managedClasspath).value, true)(ProjectLogger(baseDirectory.value.name, streams.value.log)),
        sassDepsUnforced := processSassDeps(baseDirectory.value, (Compile / managedClasspath).value, false)(ProjectLogger(baseDirectory.value.name, streams.value.log)),

        sassCompile := {
          sassDepsUnforced.value // force sassDeps to run
          runSassBuild(
            "sass build",
            "build-sass.properties",
            baseDirectory.value,
            true,
          )(
            ProjectLogger(baseDirectory.value.name, streams.value.log),
          )
        },
        (Compile / compile) := (Compile / compile).dependsOn(sassCompile).value,
    )

}
