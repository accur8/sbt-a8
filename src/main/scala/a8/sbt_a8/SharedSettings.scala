package a8.sbt_a8

import sbt._
import Keys.{sourceGenerators, _}


object SharedSettings extends SharedSettings with HaxeSettings


trait SharedSettings {

  def settings: Seq[Def.Setting[_]] =
    Seq(
      publishArtifact in (Compile, packageDoc) := false,
      publishArtifact in packageDoc := false
    )

  def jvmSettings: Seq[Def.Setting[_]] =
    Seq(
      fork := true,
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
      (resourceGenerators in Compile) += Def.task[Seq[File]] {
        a8.sbt_a8.generateBuildInfo(name.value, version.value, (resourceManaged in Compile).value)(new ProjectLogger(baseDirectory.value.name, streams.value.log))
      }
    )

  def jsSettings: Seq[Def.Setting[_]] =
     Seq(
       fork := false
     )

  def javaSettings: Seq[Def.Setting[_]] =
    Seq(
      crossPaths := false,
      autoScalaLibrary := false
    )

  def bareProject(artifactName: String, dir: java.io.File, id: String) = {
    Project(id, dir)
      .settings(settings: _*)
      .settings(Keys.name := artifactName)
  }

  def jvmProject(artifactName: String, dir: java.io.File, id: String) =
    bareProject(artifactName, dir, id)
      .settings(jvmSettings: _*)

  def javaProject(artifactName: String, dir: java.io.File, id: String) =
    jvmProject(artifactName, dir, id)
      .settings(javaSettings: _*)

}
