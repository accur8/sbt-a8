package a8.sbt_a8

import sbt.Keys._
import sbt._


object Plugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings =
    Seq(
      Compile / resourceGenerators += Def.task {
        a8.sbt_a8.generateBuildInfo(name.value, version.value, (Compile / resourceManaged).value)(ProjectLogger(baseDirectory.value.name, streams.value.log))
      }.taskValue
    )

}
