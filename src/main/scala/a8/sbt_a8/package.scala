package a8


import java.io.{File, FileInputStream}
import java.util.Properties
import scala.collection.JavaConverters._
import sbt.{Keys, Project}

package object sbt_a8 {

  def versionProps(projectDir: File): Map[String,String] = loadProperties(new File(projectDir, "version.properties"))

  def dependencyVersion(projectDir: File, name: String) = {
    versionProps(projectDir)(name)
  }

  def branchName(projectDir: File) = {
    val branchWithOrigin =
      Exec(Utilities.resolvedGitExec, "logsymbolic-ref" ,"--short", "HEAD")(None)
        .inDirectory(projectDir)
        .execCaptureOutput()
        .stdout
        .replace("-", "")
        .trim
        .split(",")
        .toList match {
          case _ :: branch :: tail => branch
        }
    branchWithOrigin.split("/", 2).last.replace("/", "")
  }

  def versionStamp(projectDir: File): String = {

    val baseVersion = versionProps(projectDir)("this")

    val buildNumber =
      Option(System.getProperty("buildNumber"))
        .map("system property" -> _)
        .getOrElse {
          val now = java.time.LocalDateTime.now()
          "generated" -> f"${now.getYear}%04d${1+now.getMonth.ordinal}%02d${now.getDayOfMonth}%02d_${now.getHour}%02d${now.getMinute}%02d_${branchName(projectDir)}"
        }

    val v = s"${baseVersion}-${buildNumber._2}"
    println(s"using version = ${v} -- ${buildNumber._1}")
    v

  }

  def generateBuildInfo(projectName: String, version: String, outputDir: File, logger: sbt.Logger): Seq[File] = {
    val bi = new BuildInfoGenerator(projectName, version, outputDir)(Some(logger))
    bi.generate()
  }

  def loadProperties(file: File): Map[String, String] = {
    if ( !file.exists() ) sys.error(s"${file.getAbsolutePath} not found")
    val props = new Properties
    props.load(new FileInputStream(file))
    props.asScala.toMap
  }

}
