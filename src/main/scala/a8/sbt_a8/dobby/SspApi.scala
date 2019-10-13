package a8.sbt_a8.dobby

trait SspApi {

  def isDobby: Boolean
  def setResponseHeader(name: String, value: String): Unit

}
