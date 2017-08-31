package a8.sbt_a8

import sbt._
import Keys.{sourceGenerators, _}


object SharedSettings extends SharedSettings


trait SharedSettings {

  def settings: Seq[Def.Setting[_]] =
    Seq(
      publishArtifact in (Compile, packageDoc) := false,
      publishArtifact in packageDoc := false
    )

  def jvmSettings: Seq[Def.Setting[_]] =
    Seq(
      fork := true,
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
    )

  def jsSettings: Seq[Def.Setting[_]] =
     Seq(
       fork := false
     )

  def bareProject(artifactName: String, dir: java.io.File, id: Option[String] = None) = {
    Project(id.getOrElse(dir.name).replaceAll("-","_"), dir)
      .settings(settings: _*)
      .settings(Keys.name := artifactName)
  }

  def jvmProject(artifactName: String, dir: java.io.File, id: Option[String] = None) =
    bareProject(artifactName, dir, id)
      .settings(jvmSettings: _*)

}