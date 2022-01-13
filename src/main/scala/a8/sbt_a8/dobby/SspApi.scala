package a8.sbt_a8.dobby

import com.typesafe.config.Config

trait SspApi {

  def isDobby: Boolean
  def reloadConfig(): Unit
  def config: Config
  def getAttribute(name: String, default: String): String
  def getBoolean(name: String, default: Boolean): Boolean
  def setResponseHeader(name: String, value: String): Unit

}
