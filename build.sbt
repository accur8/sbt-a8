import Dependencies.scalaTest
import sbt.Keys.{libraryDependencies, publishTo}

lazy val appVersion = {
  val now = java.time.LocalDateTime.now()
  val timestamp = f"${now.getYear}%02d${1+now.getMonth.ordinal}%02d${now.getDayOfMonth}%02d_${now.getHour}%02d${now.getMinute}%02d"
  val v = s"1.1.0-${timestamp}"
  println(s"setting version to ${v}")
  v
}


scalaVersion in Global := "2.12.10"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      sbtPlugin := true,
      organization := "a8",
      version      := appVersion
    )),
    resolvers += "a8-repo" at Common.readRepoUrl(),
    publishTo := Some("a8-repo-releases" at Common.readRepoUrl()),
    credentials += Common.readRepoCredentials(),
    name := "sbt-a8",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "org.freemarker" % "freemarker" % "2.3.31",
    libraryDependencies += "io.undertow" % "undertow-core" % "2.0.26.Final",
    libraryDependencies += "com.typesafe" % "config" % "1.3.3",
    libraryDependencies += "com.github.andyglow" %% "typesafe-config-scala" % "1.0.3",

  )
