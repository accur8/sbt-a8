package a8.sbt_a8

class ProjectLogger(projectName: String, delegate: sbt.Logger) {

  def debug(str: String) =
    delegate.debug(projectName + " | " + str)

  def info(str: String) =
    delegate.info(projectName + " | " + str)

  def warn(str: String) =
    delegate.warn(projectName + " | " + str)


}
