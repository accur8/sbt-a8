package example


import a8.sbt_a8.{Exec, ProjectLogger}


object GitExecFailDemo extends App {

  implicit object logger extends ProjectLogger {
    override def debug(msg: String): Unit = ()
    override def info(msg: String): Unit = ()
    override def warn(msg: String): Unit = ()
  }

  val result =
    Exec(Seq("git", "status"), Some(new java.io.File("/Users/glen/code/accur8/composite")))
      .execCaptureOutput()

  println(result)

}
