package a8.sbt_a8


object Utilities {

  def isWindows: Boolean = {
    val os: String = System.getProperty("os.name").toLowerCase
    return (os.indexOf("win") >= 0)
  }

}

