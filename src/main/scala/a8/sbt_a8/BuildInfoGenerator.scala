package a8.sbt_a8


import java.io.{BufferedReader, File, InputStreamReader, PrintStream}
import java.net.InetAddress
import java.util.Date

import sbt.IO

/**
  * generates a file called version.txt and version-details.properties to be packaged with the jar.
  * These files contain the build info useful for re-creating the dev state of a running
  * production system
  */
class BuildInfoGenerator(projectName: String, version: String, outputDir: File)(implicit logger: Option[sbt.Logger]) {

  import Utilities.resolvedGitExec

  lazy val outputBasicFile: File = new File(outputDir, "META-INF/version.properties")
  lazy val outputDetailsFile: File = new File(outputDir, "META-INF/version-details.properties")

  lazy val logMessageCount = 99
  lazy val revListCount = 99

  def generate(): Seq[File] = {
    logger.foreach(_.debug("build info generated"))
    outputResults
    logger.foreach(_.debug("build info generated"))
    Seq(outputBasicFile, outputDetailsFile)
  }

  private def outputResults = {
    outputBasicFile.getParentFile.mkdirs
    logger.foreach(_.debug("outputting results to " + outputBasicFile.getCanonicalPath))
    val localhost: InetAddress = InetAddress.getLocalHost

    val basic = s"""
build_date=${new Date}
build_machine=${localhost.getHostName}
build_user=${System.getProperty("user.name")}
"""

    IO.write(outputBasicFile, basic)

    val details = s"""
project_name=${projectName}
version_number=${version}
build_date=${new Date}
build_machine=${localhost.getHostName}
build_user=${System.getProperty("user.name")}
build_machine_ip=${InetAddress.getLocalHost.getHostAddress}
build_os=${System.getProperty("os.name")}
build_java_version=${System.getProperty("java.version")}

${pipeCommand(resolvedGitExec, "status")}
${pipeCommand(resolvedGitExec, "branch")}
${pipeCommand(resolvedGitExec, "log", "-n", "" + logMessageCount)}
${pipeCommand(resolvedGitExec, "rev-list", "-n", "" + revListCount, "HEAD")}
${pipeCommand(resolvedGitExec, "config", "-l")}
"""

    IO.write(outputDetailsFile, details)

  }

  def pipeCommand(command: String*): String = {
    val result = Exec(command).execCaptureOutput()
    val formattedText = convertToCommentedLines(result.stdout.lines.toIterable, command.mkString(" "))
    formattedText
  }

  def convertToCommentedLines(lines: Iterable[String], heading: String): String = {
    val sb: StringBuffer = new StringBuffer
    sb.append("\r\n")
    sb.append("#  ")
    sb.append(heading)
    sb.append("\r\n")
    sb.append("#\r\n")
    for (line <- lines) {
      sb.append("#  ")
      sb.append(line)
      sb.append("\r\n")
    }
    return sb.toString
  }

}