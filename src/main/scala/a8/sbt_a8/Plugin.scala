package a8.sbt_a8

import sbt.Keys._
import sbt._


object Plugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings =
    Seq(
      resourceGenerators in Compile += Def.task {
        a8.sbt_a8.generateBuildInfo(name.value, version.value, (resourceManaged in Compile).value, streams.value.log)
      }.taskValue
    )

}
