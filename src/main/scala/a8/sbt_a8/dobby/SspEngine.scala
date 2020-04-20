package a8.sbt_a8.dobby

import java.io.{PrintWriter, StringWriter}

import a8.sbt_a8.{CascadingHocon, ProjectLogger}
import com.typesafe.config.{Config, ConfigException}
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.HttpString
import org.fusesource.scalate.{Binding, DefaultRenderContext, TemplateEngine}
import org.fusesource.scalate.servlet.TemplateEngineFilter.{debug, info}
import org.fusesource.scalate.support.TemplateFinder
import com.github.andyglow.config._
import scala.sys.process.ProcessLogger

class SspHandler(webappRoot: java.io.File, delegate: HttpHandler)(implicit logger: ProjectLogger) extends HttpHandler { outer =>

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
      val hasTemplate = finder.findTemplate(path).nonEmpty
      if ( hasTemplate ) {
        exchange.dispatch(this)
        return
      } else {
        delegate.handleRequest(exchange)
        return
      }
    } else {
      processTemplate(exchange, path) match {
        case Some(content) =>
          exchange.getResponseSender.send(content)
        case None =>
          exchange.getResponseSender.send("")
      }
    }
  }

  lazy val engine: TemplateEngine = new TemplateEngine(List(webappRoot))
  lazy val finder: TemplateFinder = new TemplateFinder(engine)

  def processTemplate(exchange: HttpServerExchange, path: String): Option[String] = {
//    debug("Checking '%s'", request.getRequestURI)

    val out = new StringWriter()
    finder
      .findTemplate(path)
      .map { template =>
        debug(s"Rendering '${path}' using template '${template}'")
        val context = new DefaultRenderContext(path, engine, new PrintWriter(out))

        val api =
          new SspApi {

            override def reloadConfig(): Unit = _config = None

            override def config: Config = outer.config()

            override def getAttribute(name: String, default: String): String =
              try {
                outer
                  .config()
                  .get[Option[String]](name)
                  .getOrElse(default)
              } catch {
                case e: Exception =>
                  logger.warn(s"unable to resolve attribute ${name} -- ${e.getMessage}")
                  default
              }
            override def isDobby: Boolean = true
            override def setResponseHeader(name: String, value: String): Unit = exchange.getResponseHeaders.add(HttpString.tryFromString(name), value)
          }

        context.attributes("api") = api
        try {

          val bindings =
            List(
              Binding(
                "api",
                classOf[a8.sbt_a8.dobby.SspApi].getName,
              )
            )

          context.include(template, true, bindings)
        } catch {
          case e: Throwable =>
            e.printStackTrace(context.out)
        }
        context.out.flush()
        out.toString
      }
  }

}
