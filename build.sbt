import Dependencies._

lazy val appVersion = {
  val now = java.time.LocalDateTime.now()
  val timestamp = f"${now.getYear}%02d${1+now.getMonth.ordinal}%02d${now.getDayOfMonth}%02d_${now.getHour}%02d${now.getMinute}"
  val v = s"0.1.0-${timestamp}"
  println(s"setting version to ${v}")
  v
}

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      sbtPlugin := true,
      organization := "a8",
      scalaVersion := "2.10.6",
      version      := appVersion
    )),
    credentials += Credentials(Path.userHome / ".sbt" / "credentials"),
    publishTo := Some("Artifactory Realm" at "https://accur8.artifactoryonline.com/accur8/sbt-plugins/"),
    name := "sbt-a8",
    libraryDependencies += scalaTest % Test
  )
