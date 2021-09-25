package a8.sbt_a8.dobby

import a8.sbt_a8.dobby.FreeMarkerEngine.contentTypesByExt
import com.typesafe.config.Config
import freemarker.cache.{CacheStorage, TemplateLoader}
import freemarker.template.{Configuration, TemplateExceptionHandler, TemplateModel, TemplateScalarModel}
import io.undertow.util.{Headers, HttpString}

import java.nio.charset.Charset
import scala.collection.mutable
import com.github.andyglow.config._
import io.undertow.server.HttpServerExchange

import java.io.StringWriter

object FreeMarkerEngine {

  val version = Configuration.VERSION_2_3_29

  val contentTypesByExt =
    Map(
      "js" -> "application/x-javascript",
      "json" -> "application/json",
      "html" -> "text/html",
      "css" -> "text/css",
      "csv" -> "text/csv",
      "xml" -> "text/xml",
    )

}

class FreeMarkerEngine(
  webappRoots: Iterable[java.io.File],
  configGetter: ()=>Config,
  configResetter: ()=>Unit,
) {

  lazy val possibleTemplateExtensions =
    List(
      "",
      ".ftl",
    )

  // Create your Configuration instance, and specify if up to what FreeMarker// Create your Configuration instance, and specify if up to what FreeMarker

  // version (here 2.3.29) do you want to apply the fixes that are not 100%
  // backward-compatible. See the Configuration JavaDoc for details.
  lazy val config = newConfig()

  case class TemplateSource(name: String, file: java.io.File) {

    lazy val lastModified = file.lastModified()

    def close(): Unit = {
      readers.foreach(_.close())
    }

    final def reader(encoding: String): java.io.Reader = {
      val r = new java.io.InputStreamReader(new java.io.FileInputStream(file), Charset.forName(encoding))
      readers.append(r)
      r
    }

    val readers = mutable.Buffer[java.io.Reader]()

  }

  object templateLoader extends TemplateLoader {

    def findTemplateSource(d: java.io.File, name: String): Option[TemplateSource] =
      possibleTemplateExtensions
        .map(e => new java.io.File(d, name + e))
        .filter(_.exists)
        .headOption
        .map { f =>
          TemplateSource(name, f.getCanonicalFile)
        }

    def findTemplateSourceOpt(name: String): Option[TemplateSource] = {
      webappRoots
        .iterator
        .flatMap { d =>
          findTemplateSource(d, name)
        }
        .take(1)
        .toList
        .headOption
    }

    override def findTemplateSource(name: String): TemplateSource =
      findTemplateSourceOpt(name)
        .orNull

    override def getLastModified(templateSource: Any): Long =
      withTemplateSource(templateSource)(_.lastModified)

    override def getReader(templateSource: Any, encoding: String): java.io.Reader =
      withTemplateSource(templateSource)(_.reader(encoding))

    override def closeTemplateSource(templateSource: Any): Unit =
      withTemplateSource(templateSource)(_.close())

    def withTemplateSource[A](templateSource: Any)(fn: TemplateSource=>A): A = {
      templateSource match {
        case ts: TemplateSource =>
          fn(ts)
        case _ =>
          sys.error(s"${templateSource} is not a file")
      }
    }

  }

  def newConfig() = {

    val cfg = new Configuration(FreeMarkerEngine.version)

    // Specify the source where the template files come from. Here I set a
    // plain directory for it, but non-file-system sources are possible too:
    cfg.setTemplateLoader(templateLoader)

    // From here we will set the settings recommended for new projects. These
    // aren't the defaults for backward compatibilty.

    // Set the preferred charset template files are stored in. UTF-8 is
    // a good choice in most applications:
    cfg.setDefaultEncoding("UTF-8")

    // Sets how errors will appear.
    // During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)

    // Don't log exceptions inside FreeMarker that it will thrown at you anyway:
    cfg.setLogTemplateExceptions(false)

    // Wrap unchecked exceptions thrown during template processing into TemplateException-s:
    cfg.setWrapUncheckedExceptions(false)

    // Do not fall back to higher scopes when reading a null loop variable:
    cfg.setFallbackOnNullLoopVariable(false)

    cfg

  }

  def parseExtension(templateName: String): String = {
    val parts = templateName.split("\\.").filterNot(_.equalsIgnoreCase("ftl"))
    parts.lastOption.getOrElse("")
  }

  def processTemplate(path: String, exchange: HttpServerExchange): String = {

    object sspApi extends SspApi {

      override def reloadConfig(): Unit = configResetter()

      override def config: Config = configGetter()

      override def getAttribute(name: String, default: String): String =
        try {
          configGetter()
            .get[Option[String]](name)
            .getOrElse(default)
        } catch {
          case e: Exception =>
            System.err.println(s"unable to resolve attribute ${name} -- ${e.getMessage}")
            default
        }
      override def isDobby: Boolean = true
      override def setResponseHeader(name: String, value: String): Unit = exchange.getResponseHeaders.add(HttpString.tryFromString(name), value)
    }

    val model =
      new ScalaObjectWrapper().wrap(
        Map(
          "api" -> sspApi,
        )
      )

    val extension = parseExtension(path)

    contentTypesByExt
      .get(extension)
      .foreach(contentType => exchange.getResponseHeaders.add(Headers.CONTENT_TYPE, contentType))

    val templateSource = templateLoader.findTemplateSource(path)

    val template = newConfig().getTemplate(path)

    val sw = new StringWriter
    template.process(model, sw)

    sw.toString

  }


  def lazyScalar[A](fn: =>A): TemplateScalarModel =
    new TemplateScalarModel {
      override def getAsString: String = fn.toString
    }

}
