package a8.sbt_a8

import java.nio.file.Path

import scala.reflect.ClassTag
import com.typesafe.config._

import scala.language.implicitConversions

object HoconOps extends HoconOps

/**
  * this is a copy of the class of the same name in Odin
  */
trait HoconOps {


  def parseHocon(hocon: String, syntax: ConfigSyntax = ConfigSyntax.CONF): Config = {

    val parseOptions =
      ConfigParseOptions.
        defaults.
        setAllowMissing(false).
        setSyntax(syntax)

    ConfigFactory.parseString(hocon, parseOptions)

  }

  object impl {

    def loadConfig(file: Path): Config = {
      val config = ConfigFactory.parseFile(file.toFile)
      config
    }

//    def toJsValue(conf: Config): JsValue = {
//      toJsObject(conf.root())
//    }
//
//    def toJsObject(co: ConfigObject): JsObject = {
//      new JsObject(
//        co.unwrapped.keySet.asScala
//          .map { fieldName =>
//            val jsv = impl.toJsValue(co.get(fieldName))
//            fieldName -> jsv
//          }
//          .toMap
//      )
//    }
//
//    def toJsValue(cv: ConfigValue): JsValue = {
//      (cv, cv.valueType()) match {
//        case (cl: ConfigList, _) => {
//          JsArray(cl.iterator().asScala.toList.map(toJsValue))
//        }
//        case (co: ConfigObject, _) => impl.toJsObject(co)
//        case (_, ConfigValueType.BOOLEAN) => JsBoolean(cv.unwrapped.asInstanceOf[java.lang.Boolean])
//        case (_, ConfigValueType.NUMBER) =>
//          JsNumber(BigDecimal(cv.unwrapped.asInstanceOf[java.lang.Number].toString))
//        case (_, ConfigValueType.NULL) => JsNull
//        case (_, ConfigValueType.STRING) => JsString(cv.unwrapped.asInstanceOf[java.lang.String])
//        case (x, _) => sys.error("should never have gotten here " + x)
//      }
//    }

  }

//  implicit def implicitConfigOps(config: Config) = new ConfigOps(config)
//  class ConfigOps(private val config: Config) {
//
//    def read[A : Reads : ClassTag]: A = {
//      val jsv = impl.toJsValue(config.root)
//      JsonAssist.fromJson[A](jsv)
//    }
//
//    def readPath[A : Reads : ClassTag](path: String): A = {
//      val jsv = impl.toJsValue(config.getValue(path))
//      JsonAssist.fromJson[A](jsv)
//    }
//
//  }

//  implicit def implicitConfigValueOps(configValue: ConfigValue) = new ConfigValueOps(configValue)
//  class ConfigValueOps(private val configValue: ConfigValue) {
//
//    def asJsValue = impl.toJsValue(configValue)
//
//    def read[A : Reads : ClassTag]: A =
//      JsonAssist.fromJson[A](asJsValue)
//
//    def readPath[A : Reads : ClassTag](path: String): A = {
//      configValue match {
//        case co: ConfigObject =>
//          co.toConfig.readPath[A](path)
//      }
//    }
//
//  }


}
