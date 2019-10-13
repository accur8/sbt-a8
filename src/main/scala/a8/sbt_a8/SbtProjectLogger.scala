package a8.sbt_a8

class SbtProjectLogger(projectName: String, delegate: sbt.Logger) extends ProjectLogger {

  override def debug(str: String) =
    delegate.debug(projectName + " | " + str)

  override def info(str: String) =
    delegate.info(projectName + " | " + str)

  override def warn(str: String) =
    delegate.warn(projectName + " | " + str)


}
