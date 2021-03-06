package a8.sbt_a8.dobby

import _root_.freemarker.template._
import java.util.Date
import xml._
import java.lang.reflect.{Modifier, Field, Method}

object ScalaObjectWrapper {

  val resolveFields = true
  val resolveMethods = true
  val delegateToDefault = true
  val defaultWrapper = new DefaultObjectWrapperBuilder(FreeMarkerEngine.version).build()

  def unwrap(obj: Any): Any =
    obj match {
      case null => null
      case scalar: TemplateScalarModel => scalar.getAsString
      case bool: TemplateBooleanModel => bool.getAsBoolean.booleanValue()
      case _ => sys.error(s"Unwrapping ${obj.getClass.getName} is unsupported")
    }

}

class ScalaObjectWrapper extends ObjectWrapper {

  override def wrap(obj: Any): TemplateModel = obj match {

    // Basic types
    case null => null
    case option: Option[_] => option match {
      case Some(o) => wrap(o)
      case _ => null
    }
    case model: TemplateModel => model
    // Circumflex model types
    //case wrapper: Wrapper[_] => wrap(wrapper.item)
    // Scala base types
//    case xml: NodeSeq => new ScalaXmlWrapper(xml, this)
    case seq: Seq[_] => new ScalaSeqWrapper(seq, this)
    case map: scala.collection.Map[_, _] => new ScalaMapWrapper(map.map(p =>(p._1.toString, p._2)), this)
    case it: Iterable[_] => new ScalaIterableWrapper(it, this)
    case it: Iterator[_] => new ScalaIteratorWrapper(it, this)
    case str: String => new SimpleScalar(str)
    case date: Date => new ScalaDateWrapper(date, this)
    case num: Number => new SimpleNumber(num)
    case bool: Boolean => if (bool) TemplateBooleanModel.TRUE else TemplateBooleanModel.FALSE
    // Everything else
    case o => new ScalaBaseWrapper(o, this)
  }

}

class ScalaDateWrapper(val date: Date, wrapper: ObjectWrapper)
  extends ScalaBaseWrapper(date, wrapper) with TemplateDateModel {
  def getDateType = TemplateDateModel.UNKNOWN
  def getAsDate = date
}

class ScalaSeqWrapper[T](val seq: Seq[T], wrapper: ObjectWrapper)
  extends ScalaBaseWrapper(seq, wrapper) with TemplateSequenceModel {
  def get(index: Int) = wrapper.wrap(seq(index))
  def size = seq.size
}

class ScalaMapWrapper(val map: collection.Map[String, _], wrapper: ObjectWrapper)
  extends ScalaBaseWrapper(map, wrapper) with TemplateHashModelEx {
  override def get(key: String): TemplateModel = wrapper.wrap(map.get(key).orElse(Some(super.get(key))))
  override def isEmpty = map.isEmpty
  def values = new ScalaIterableWrapper(map.values, wrapper)
  val keys = new ScalaIterableWrapper(map.keys, wrapper)
  def size = map.size
}

class ScalaIterableWrapper[T](val it: Iterable[T], wrapper: ObjectWrapper)
  extends ScalaBaseWrapper(it, wrapper) with TemplateCollectionModel {
  def iterator = new ScalaIteratorWrapper(it.iterator, wrapper)
}

class ScalaIteratorWrapper[T](val it: Iterator[T], wrapper: ObjectWrapper)
  extends ScalaBaseWrapper(it, wrapper) with TemplateModelIterator with TemplateCollectionModel {
  def next = wrapper.wrap(it.next())
  def hasNext = it.hasNext
  def iterator = this
}

class ScalaMethodWrapper(val target: Any, val methodName: String, val wrapper: ObjectWrapper) extends TemplateMethodModelEx {
  import scala.reflect.runtime.{universe => ru}
  val im = ru.runtimeMirror(target.getClass.getClassLoader).reflect(target)
  val methodSymbol = im.symbol.typeSignature.member(ru.TermName(methodName))
  val method = im.reflectMethod(methodSymbol.asMethod)

  override def exec(arguments: java.util.List[_]): Object = {
    wrapper.wrap(method(arguments.toArray.map(ScalaObjectWrapper.unwrap): _*))
  }
}

class ScalaXmlWrapper(val node: NodeSeq, val wrapper: ObjectWrapper)
  extends TemplateNodeModel
    with TemplateHashModel
    with TemplateSequenceModel
    with TemplateScalarModel {
  // as node
  def children: Seq[Node] = node match {
    case node: Elem => node.child.flatMap {
      case e: Elem => Some(e)
      case a: Attribute => Some(a)
      case t: Text => if (t.text.trim == "") None else Some(t)
      case _ => None
    }
    case _ => Nil
  }
  def getNodeNamespace: String = node match {
    case e: Elem => e.namespace
    case _ => ""
  }
  def getNodeType: String = node match {
    case e: Elem => "element"
    case t: Text => "text"
    case a: Attribute => "attribute"
    case _ => null
  }
  def getNodeName: String = node match {
    case e: Elem => e.label
    case _ => null
  }
  def getChildNodes: TemplateSequenceModel = new ScalaSeqWrapper[Node](children, wrapper)
  // due to immutability of Scala XML API, nodes are unaware of their parents.
  def getParentNode: TemplateNodeModel = new ScalaXmlWrapper(null, wrapper)
  // as hash
  def isEmpty: Boolean = node.size == 0
  def get(key: String): TemplateModel = {
    val children = node \ key
    if (children.size == 0) wrapper.wrap(None)
    if (children.size == 1) wrapper.wrap(children(0))
    else wrapper.wrap(children)
  }
  // as sequence
  def size: Int = node.size
  def get(index: Int): TemplateModel = new ScalaXmlWrapper(node(index), wrapper)
  // as scalar
  def getAsString: String = node.text
}

class ScalaBaseWrapper(val obj: Any, val wrapper: ObjectWrapper)
  extends TemplateHashModel with TemplateScalarModel {

  val objectClass = obj.asInstanceOf[Object].getClass

  private def findMethod(clazz: Class[_], name: String): Option[Method] = {
    val methods = clazz.getMethods
    methods
      .find { m =>
        m.getName == name && Modifier.isPublic(m.getModifiers)
      } match {
      case None if clazz != classOf[Object] =>
        findMethod(clazz.getSuperclass, name)
      case other => other
    }
  }

  private def findField(cl: Class[_], name: String): Option[Field] =
    cl.getFields.toList.find { f =>
      f.getName.equals(name) && Modifier.isPublic(f.getModifiers)
    } match {
      case None if cl != classOf[Object] => findField(cl.getSuperclass, name)
      case other => other
    }

  def get(key: String): TemplateModel = {
    val o = obj.asInstanceOf[Object]
    if (ScalaObjectWrapper.resolveFields)
      findField(objectClass, key) match {
        case Some(field) => return wrapper.wrap(field.get(o))
        case _ =>
      }
    if (ScalaObjectWrapper.resolveMethods)
      findMethod(objectClass, key) match {
        case Some(method) if (method.getParameterTypes.length == 0) =>
          return wrapper.wrap(method.invoke(obj))
        case Some(method) =>
          return new ScalaMethodWrapper(obj, method.getName, wrapper)
        case _ =>
      }
    // nothing found
    wrapper.wrap(null)
  }

  def isEmpty = false

  def getAsString = obj.toString
}
