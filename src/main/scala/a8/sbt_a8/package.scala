package a8


import java.io.{File, FileInputStream}
import java.util.Properties
import scala.collection.JavaConverters._
import sbt.{Keys, Project}

package object sbt_a8 {

  lazy val skipGit: Boolean =
    System
      .getProperty("skipGit", "false")
      .toBoolean

  def versionProps(projectDir: File): Map[String,String] = loadProperties(new File(projectDir, "version.properties"))

  def dependencyVersion(projectDir: File, name: String) = {
    versionProps(projectDir)(name)
  }

  implicit class StringOps(s: String) {
    def splitList(regex: String): List[String] =
      s.split(regex).toList.map(_.trim).filter(_.length > 0)
  }

  def parseGitBranchName(gitLogStdout: String): String = {
    val trimmedGitLogStdout = gitLogStdout.trim
    try {
      val bn =
        (trimmedGitLogStdout
          .replace(")", "")
          .replace("(", "")
          .splitList(",")
          .map(_.trim)
          .filter(b => b != "HEAD" && b != "origin/HEAD")
          .map { s =>
            s.splitList("->") match {
              case _ :: b :: Nil => b
              case l => l.head
            }
          }
          .headOption
          .getOrElse {
            println(s"unable to parse branch name from '${trimmedGitLogStdout}'")
            "unknown"
          }) match {
            case b if b.startsWith("origin/") => b.substring("origin/".length)
            case b => b
          }
      scrubBranchName(bn)
    } catch {
      case e: Exception =>
        println(s"unable to parse branch name from '${trimmedGitLogStdout}'")
        "unknown"
    }
  }

  def scrubBranchName(unscrubbedName: String): String = {
    unscrubbedName
      .filter(ch => ch.isLetterOrDigit)
      .toLowerCase
  }

  def branchName(projectDir: File)(implicit logger: ProjectLogger): String = {
    if ( skipGit ) {
      "skipgit"
    } else {
      val gitLogStdout = Exec(Utilities.resolvedGitExec, "log", "-n", "1", "--pretty=%d", "HEAD")
        .inDirectory(projectDir)
        .execCaptureOutput()
        .stdout
      parseGitBranchName(gitLogStdout)
    }
  }

  def versionStamp(projectDir: File): String = {

    implicit val logger = ProjectLogger("root", sbt.Logger.Null)

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

  def generateBuildInfo(projectName: String, version: String, outputDir: File)(implicit logger: ProjectLogger): Seq[File] = {
    val bi = new BuildInfoGenerator(projectName, version, outputDir)
    bi.generate()
  }

  def loadProperties(file: File): Map[String, String] = {
    if ( !file.exists() ) sys.error(s"${file.getAbsolutePath} not found")
    val props = new Properties
    props.load(new FileInputStream(file))
    props.asScala.toMap
  }

}
