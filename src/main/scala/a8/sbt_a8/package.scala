package a8


import java.io.File
import sbt.{Keys, Project}

package object sbt_a8 {

  def versionStamp(baseVersion: String): String = {
    val now = java.time.LocalDateTime.now()
    val timestamp = f"${now.getYear-2000}%02d${1+now.getMonth.ordinal}%02d${now.getDayOfMonth}%02d${now.getHour}%02d${now.getMinute}%02d${now.getSecond}%02d"
    val v = s"${baseVersion}-${timestamp}"
    println(s"using version = ${v}")
    v
  }

  def generateBuildInfo(projectName: String, version: String, outputDir: File, logger: sbt.Logger): Seq[File] = {
    val bi = new BuildInfoGenerator(projectName, version, outputDir)(logger)
    bi.generate()
  }

}
