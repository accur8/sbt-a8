import Dependencies.{scalaTest, _}
import sbt.Credentials
import sbt.Keys.{credentials, libraryDependencies, publishTo}

lazy val appVersion = {
  val now = java.time.LocalDateTime.now()
  val timestamp = f"${now.getYear}%02d${1+now.getMonth.ordinal}%02d${now.getDayOfMonth}%02d_${now.getHour}%02d${now.getMinute}%02d"
  val v = s"1.1.0-${timestamp}"
  println(s"setting version to ${v}")
  v
}


scalaVersion in Global := "2.12.10"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      sbtPlugin := true,
      organization := "a8",
      version      := appVersion
    )),
    resolvers += "a8-repo" at readRepoUrl(),
    publishTo := Some("a8-repo-releases" at "s3://s3-us-east-1.amazonaws.com/a8-artifacts/releases"),
    s3CredentialsProvider := { (bucket: String) =>
      import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
      new AWSStaticCredentialsProvider(new BasicAWSCredentials(readRepoProperty("publish_aws_access_key"), readRepoProperty("publish_aws_secret_key")))
    },
    name := "sbt-a8",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "io.undertow" % "undertow-core" % "2.0.26.Final",
    libraryDependencies += "org.scalatra.scalate" % "scalate-core_2.12" % "1.9.5",
    libraryDependencies += "com.typesafe" % "config" % "1.3.3",
    libraryDependencies += "com.github.andyglow" %% "typesafe-config-scala" % "1.0.3",

  )



  def readRepoUrl() = readRepoProperty("repo_url")

  def readRepoProperty(propertyName: String): String = {
    import scala.collection.JavaConverters._
    import java.io.FileInputStream
    val props = new java.util.Properties()
    val configFile = new java.io.File(System.getProperty("user.home") + "/.a8/repo.properties")
    if ( configFile.exists() ) {
      val input = new FileInputStream(configFile)
      try {
        props.load(input)
      } finally {
        input.close()
      }
      props.asScala.get(propertyName) match {
        case Some(s) =>
          s
        case None =>
          sys.error("could not find property " + propertyName + " in " + configFile )
      }
    } else {
      sys.error("config file " + configFile + " does not exist")
    }
  }


  def readRepoCredentials(): Credentials = {
    val repoUrl = new java.net.URL(readRepoUrl())
    Credentials(
      readRepoProperty("repo_realm"),
      repoUrl.getHost,
      readRepoProperty("repo_user"),
      readRepoProperty("repo_password"),
    )
  }
