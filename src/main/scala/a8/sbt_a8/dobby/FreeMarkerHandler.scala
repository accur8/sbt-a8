package a8.sbt_a8.dobby

import java.io.{PrintWriter, StringWriter}

import a8.sbt_a8.{CascadingHocon, ProjectLogger}
import com.typesafe.config.{Config, ConfigException}
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.HttpString
import com.github.andyglow.config._
import scala.sys.process.ProcessLogger

class FeeMarkerHandler(webappRoot: java.io.File, delegate: HttpHandler)(implicit logger: ProjectLogger) extends HttpHandler { outer =>

  var _config: Option[Config] = None

  def config(): Config = {
    _config match {
      case None =>
        val c = CascadingHocon.loadConfigsInDirectory(webappRoot.toPath)
        _config = Some(c)
        c
      case Some(c) =>
        c
    }
  }

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    val path = exchange.getRequestPath
    if (exchange.isInIoThread) {
      val hasTemplate = engine.templateLoader.findTemplateSourceOpt(path).nonEmpty
      if ( hasTemplate ) {
        exchange.dispatch(this)
        return
      } else {
        delegate.handleRequest(exchange)
        return
      }
    } else {
      val responseBody = engine.processTemplate(path, exchange)
      exchange.getResponseSender.send(responseBody)
    }
  }

  lazy val engine: FreeMarkerEngine = new FreeMarkerEngine(List(webappRoot), () => config(), () => _config = None)

}
