package a8.sbt_a8.dobby

import io.undertow.Undertow
import io.undertow.server.handlers.resource.PathResourceManager
import io.undertow.Handlers
import java.nio.file.{Path, Paths}

import a8.sbt_a8.ProjectLogger

import scala.collection.JavaConverters._
import scala.collection.mutable

class WebServer(webappRoot: Path, port: Int = 8000, host: String = "localhost")(implicit logger: ProjectLogger) {

  lazy val start = {
    logger.info(s"starting Dobby server listening on port ${port} webappRoot of ${webappRoot}")
    server.start()
  }

  def stop(): Unit = {
    logger.info(s"stopping Dobby server listening on port ${port} webappRoot of ${webappRoot}")
    server.stop()
  }

  lazy val server =
    Undertow.builder()
      .addHttpListener(port, host)
      .setHandler(resourceHandler)
      .build()

  lazy val resourceHandler = {
    val followSymlinks = true
    new SspHandler(
      webappRoot.toFile,
      Handlers.resource(
        new PathResourceManager(webappRoot, 100, followSymlinks, Seq[String](): _*)
      ).setDirectoryListingEnabled(true)
    )
  }

}
