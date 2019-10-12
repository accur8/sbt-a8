package a8.sbt_a8

import a8.sbt_a8.Utilities._
import net.model3.servlet.runner.JettyRunner
import sbt.Keys._
import sbt._

/*


*/

trait WorkbenchSettings { self: SharedSettings =>

  lazy val workbenchStart = taskKey[Unit]("Start Workbench Web Server")
  lazy val workbenchGenerate = taskKey[Unit]("Generate webapp-composite folder")

  def processStart(projectRoot: java.io.File)(implicit logger: ProjectLogger): Unit = {
    JettyRunner.main(Array())
  }


  def processGenerate(projectRoot: java.io.File, jars1: Iterable[Attributed[java.io.File]], jars2: Iterable[Attributed[java.io.File]], force: Boolean)(implicit logger: ProjectLogger): Unit = {

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

  def workbenchSettings: Seq[Def.Setting[_]] =
    Seq(

      workbenchGenerate := processGenerate(baseDirectory.value, (managedClasspath in Compile).value, (managedClasspath in Test).value, true)(new ProjectLogger(baseDirectory.value.name, streams.value.log)),

      workbenchStart := {
        processStart(baseDirectory.value)(new ProjectLogger(baseDirectory.value.name, streams.value.log))
      },

    )

}