package a8.sbt_a8

import java.io.{ByteArrayOutputStream, PrintWriter}

import sbt.File


object Exec {

  def apply(args: String*)(implicit logger: Option[sbt.Logger]): Exec =
    Exec(args, None)

  case class Result(
    exitCode: Int,
    stdout: String,
    stderr: String
  )

}

case class Exec(
  args: Iterable[String],
  workingDirectory: Option[File] = None
) (
  implicit logger: Option[sbt.Logger]
) {

  def inDirectory(directory: File) =
    copy(workingDirectory = Some(directory))

  import Exec._

  private def _process =
    sys.process.Process(args.toSeq, workingDirectory.map(d => new java.io.File(d.getAbsolutePath)))

  def execCaptureOutput(failOnNonZeroExitCode: Boolean = true): Result = {
    import sys.process._
    val stdout = new ByteArrayOutputStream
    val stderr = new ByteArrayOutputStream
    val stdoutWriter = new PrintWriter(stdout)
    val stderrWriter = new PrintWriter(stderr)
    logger.foreach(_.debug(toString))
    val exitCode = _process.!(ProcessLogger(stdoutWriter.println, stderrWriter.println))
    stdoutWriter.close()
    stderrWriter.close()
    val result = Result(
      exitCode = exitCode,
      stdout = stdout.toString,
      stderr = stderr.toString
    )
    if ( failOnNonZeroExitCode && exitCode != 0 )
      sys.error(s"error running \n    ${this}\n    ${result}")
    result
  }

  def execInline(failOnNonZeroExitCode: Boolean = true): Int = {
    logger.foreach(_.debug(toString))
    val exitCode = _process.!
    if ( failOnNonZeroExitCode && exitCode != 0 )
      sys.error(s"error running ${this}")
    exitCode
  }

  lazy val argsAsString =
    args
      .map { arg =>
        if ( arg.exists(_.isWhitespace) ) s"'${arg}'"
        else arg
      }
      .mkString(" ")

  override def toString =
    s"running ${workingDirectory.map(d=>s"with a cwd of ${d}").getOrElse("")} the command -- ${argsAsString}"

}
