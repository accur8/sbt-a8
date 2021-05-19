
Global / scalaVersion := "2.12.10"
Global / sbtPlugin := true
Global / organization := "io.accur8"
Global / version := "1.2.0"

/** Sonatype Publishing Start */
publishTo := sonatypePublishToBundle.value
sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
credentials += Credentials(Path.userHome / ".sbt" / "sonatype.credentials")
publishMavenStyle := true
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/renns/sbt-a8"))
scmInfo := Some(ScmInfo(url("https://github.com/renns/sbt-a8"), "scm:git@github.com:renns/sbt-a8.git"))
developers := List(
  Developer(id="fizzy33", name="Glen Marchesani", email="glen@accur8software.com", url=url("https://github.com/fizzy33")),
  Developer(id="renns", name="Raphael Enns", email="raphael@accur8software.com", url=url("https://github.com/renns")),
)
/** Sonatype Publishing End */

lazy val root = (project in file("."))
  .settings(
    name := "sbt-a8",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "io.undertow" % "undertow-core" % "2.0.26.Final",
      "org.scalatra.scalate" % "scalate-core_2.12" % "1.9.5",
      "com.typesafe" % "config" % "1.3.3",
      "com.github.andyglow" %% "typesafe-config-scala" % "1.0.3",
    )
  )
