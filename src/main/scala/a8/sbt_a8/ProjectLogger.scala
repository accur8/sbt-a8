package a8.sbt_a8

object ProjectLogger {


  def apply(projectName: String, delegate: sbt.Logger): ProjectLogger =
    new SbtProjectLogger(projectName, delegate)

  class SbtProjectLogger(projectName: String, delegate: sbt.Logger) extends ProjectLogger {

    override def debug(str: String) =
      delegate.debug(projectName + " | " + str)

    override def info(str: String) =
      delegate.info(projectName + " | " + str)

    override def warn(str: String) =
      delegate.warn(projectName + " | " + str)

  }

}


trait ProjectLogger {

  def debug(msg: String): Unit
  def info(msg: String): Unit
  def warn(msg: String): Unit

}
