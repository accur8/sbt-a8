package a8.sbt_a8


object Utilities {

  def isWindows: Boolean = {
    val os: String = System.getProperty("os.name").toLowerCase
    return (os.indexOf("win") >= 0)
  }

  lazy val resolvedGitExec: String =
    if (Utilities.isWindows) "git.exe"
    else "git"

  lazy val workingPhantomjsInstall: Boolean = {
    import sys.process._
    try {
      val exitCode: Int = Process(List("phantomjs", "-v")).!
      exitCode == 0
    } catch {
      case e: java.io.IOException =>
        // command not found
        false
    }
  }

  def using[T <: java.io.Closeable, R](resource: T)(block: T => R): R = {
    try { block(resource) }
    finally { resource.close() }
  }


  implicit class FileOps(file: java.io.File) {
    def write(bytes: Array[Byte]): Unit =
      using(new java.io.FileOutputStream(file))(_.write(bytes))
  }

  implicit class InputStreamOps(is: java.io.InputStream) {
    def toByteArray: Array[Byte] = {
      using(is) { _ =>
        sbt.IO.readBytes(is)
      }
    }
  }

}

