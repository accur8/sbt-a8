package a8.sbt_a8

import sbt._
import Keys._
import Utilities._

/*

  TODO move into sbt-a8
  TODO figure out ordering issues (compile tests before they run)
  DONE copy haxe source to target/classes/haxe-src
  DONE test run

*/

trait HaxeSettings { self: SharedSettings =>

  object impl {

    private var _workingPhantomjsInstall = none[Boolean]

    def workingPhantomjsInstall(implicit logger: ProjectLogger): Boolean = {
      if ( _workingPhantomjsInstall.isEmpty )
        _workingPhantomjsInstall = Some(
          try {
            val results = Exec("phantomjs", "-v").execCaptureOutput(false)
            results.exitCode == 0
          } catch {
            case e: java.io.IOException =>
              // command not found
              false
          }
        )
      _workingPhantomjsInstall.get
    }

    lazy val haxeSrcJarPath = "haxe-src"

  }

  lazy val haxeCompile = taskKey[Unit]("Compile Haxe Code")
  lazy val haxeTestCompile = taskKey[Unit]("Compile Haxe Test Code")
  lazy val haxeTestsRun = taskKey[Unit]("Run Haxe Tests")
  lazy val haxeDeps = taskKey[Unit]("Generate src-deps folder")
  lazy val haxeDepsUnforced = taskKey[Unit]("Generate src-deps folder if it doesn't already exist")

  def haxeProject(name: String, dir: java.io.File) =
    bareProject(dir.name, dir)
      .settings(settings: _*)
      .settings(haxeSettings: _*)
      .settings(
        Keys.name := name,
      )

  def runHaxeTests(projectRoot: java.io.File)(implicit logger: ProjectLogger): Unit = {

    import impl._
    val skipHaxeTests = false
    val testRunnerJs = projectRoot / "tests-automated-runner.js"

    if ( !skipHaxeTests ) {
      if (!workingPhantomjsInstall && testRunnerJs.exists) {
        sys.error("no phantomjs install found and this is a build which requires phantomjs")
      }
      if (testRunnerJs.exists) {
        if (workingPhantomjsInstall) {

          import sys.process._

          val processLogger = ProcessLogger(line => logger.info(line), line => logger.warn(line))

          logger.debug(s"running -- phantomjs ${testRunnerJs.name} -- in cwd -- ${projectRoot}")

          val pb: ProcessBuilder = Process(List("phantomjs", testRunnerJs.name), Some(projectRoot))

          val process = pb.run(processLogger)

          if (process.exitValue() == 0) {
          } else {
            sys.error(s"automated unit tests exec failed with exit code ${process.exitValue()}.  See previous messages in the logs for details.")
          }
        } else {

          logger.info("skipping running of haxe unit tests since no valid phantomjs install found")

        }

      } else {
        logger.info(s"skipping running of haxe unit tests no tests-automated-runner.js found")
      }
    } else {
      logger.info(s"skipping running of haxe unit tests since skipHaxeTests = ${skipHaxeTests}")
    }

  }


  def processHaxeDeps(projectRoot: java.io.File, jars1: Iterable[Attributed[java.io.File]], jars2: Iterable[Attributed[java.io.File]], force: Boolean)(implicit logger: ProjectLogger): Unit = {

    val jars: Iterable[java.io.File] = (jars1 ++ jars2).map(_.data).toList.distinct

    import java.util.jar._
    import scala.collection.JavaConverters._

    val srcDepsDir = projectRoot / "src-deps"

    if ( force ) {
      sbt.IO.delete(srcDepsDir)
    }

    if ( srcDepsDir.exists ) {
      logger.info(s"${impl.haxeSrcJarPath} already exists no action taken ${srcDepsDir}")
    } else {
      srcDepsDir.mkdirs()
      val prefixes = List(impl.haxeSrcJarPath + "/", "webapp/")
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

  def runHaxeBuild(context: String, buildFile: String, projectRoot: java.io.File, copySources: Boolean = false)(implicit logger: ProjectLogger): Unit = {
    if ( (projectRoot / buildFile).exists ) {
      val results =
        a8.sbt_a8
          .Exec("haxe", buildFile)(logger)
          .inDirectory(projectRoot)
          .execCaptureOutput()
      val resultLines =
        (results.stdout.lines ++ results.stderr.lines).filter(_.trim.nonEmpty) match {
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
        val sourcesTo = projectRoot / s"target/scala-2.12/classes/${impl.haxeSrcJarPath}"

        logger.info(s"copying haxe sources from ${sourcesFrom} into ${sourcesTo}")
        sourcesTo.mkdirs

        sbt.IO.copyDirectory(sourcesFrom, sourcesTo, overwrite = true, preserveLastModified = false)

      }

    } else {
      logger.info(s"${context} in ${projectRoot} -- build file ${buildFile} not found")
    }
  }




  def haxeSettings: Seq[Def.Setting[_]] =
    Seq(

        haxeDeps := processHaxeDeps(baseDirectory.value, (managedClasspath in Compile).value, (managedClasspath in Test).value, true)(new ProjectLogger(baseDirectory.value.name, streams.value.log)),
        haxeDepsUnforced := processHaxeDeps(baseDirectory.value, (managedClasspath in Compile).value, (managedClasspath in Test).value, false)(new ProjectLogger(baseDirectory.value.name, streams.value.log)),

        haxeTestsRun := {
          haxeTestCompile.value
          runHaxeTests(baseDirectory.value)(new ProjectLogger(baseDirectory.value.name, streams.value.log))
        },

        haxeCompile := {
          haxeDepsUnforced.value // force haxeDeps to run
          runHaxeBuild(
            "haxe build",
            "build.hxml",
            baseDirectory.value,
            true,
          )(
            new ProjectLogger(baseDirectory.value.name, streams.value.log),
          )
        },
        (compile in Compile) := (compile in Compile).dependsOn(haxeCompile).value,

        haxeTestCompile := {
          haxeDepsUnforced.value // force haxeDeps to run
          runHaxeBuild(
            "haxe test build",
            "tests-build.hxml",
            baseDirectory.value,
          )(
            new ProjectLogger(baseDirectory.value.name, streams.value.log),
          )
        },
        (test in Test) := (test in Test).dependsOn(haxeTestsRun).value,

    )

}