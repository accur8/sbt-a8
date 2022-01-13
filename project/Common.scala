import sbt.Credentials

object Common {

  def readRepoUrl() = readRepoProperty("repo_url")

  lazy val repoConfigFile = new java.io.File(System.getProperty("user.home") + "/.a8/repo.properties")

  lazy val repoProperties = {
    import scala.collection.JavaConverters._
    val props = new java.util.Properties()
    if ( repoConfigFile.exists() ) {
      val input = new java.io.FileInputStream(repoConfigFile)
      try {
        props.load(input)
      } finally {
        input.close()
      }
      props.asScala
    } else {
      sys.error("config file " + repoConfigFile + " does not exist")
    }
  }

  def readRepoProperty(propertyName: String): String = {
    repoProperties.get(propertyName) match {
      case Some(s) =>
        s
      case None =>
        sys.error("could not find property " + propertyName + " in " + repoConfigFile)
    }
  }

  def readRepoCredentials(): Credentials = {
    val repoUrl = new java.net.URL(readRepoUrl())
    Credentials(
      readRepoProperty("repo_realm"),
      repoUrl.getHost,
      readRepoProperty("repo_user"),
      readRepoProperty("repo_password")
    )
  }

}
