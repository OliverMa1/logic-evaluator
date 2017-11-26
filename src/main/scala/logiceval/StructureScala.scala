package logiceval

import scala.collection.JavaConversions

/**
  * Structure for evaluating queries
  */
abstract class StructureScala {
// TODO
  /**
    * returns all values for the given type
    */
  def valuesForCustomType(typ: CustomType): java.lang.Iterable[Any]


  /**
    * returns the interpretation of a constant
    */
  def interpretConstant(constantName: String, args: Array[Any]): Any


  def values(typ: Type): java.lang.Iterable[Any] =
    JavaConversions.asJavaIterable(valuesStream(typ))

  def valuesStream(typ: Type): Stream[Any] = null/*typ match {
    case SetType(elementType) =>
      throw new RuntimeException("Set-type is not enumerable")
    case MapType(keyType, valueType) =>
      throw new RuntimeException("Map-type is not enumerable")
    case DataType(name, constructors) =>
      for (constr <- constructors.toStream; vals <- valuesList(constr.fields)) yield {
        DatatypeValue(constr.name, vals)
      }
    case ct: CustomType =>
      JavaConversions.iterableAsScalaIterable(valuesForCustomType(ct)).toStream
  }*/

  def valuesList(list: Seq[Type]): Stream[List[Any]] = list match {
    case Nil =>
      Stream(List())
    case List(t) =>
      for (v <- valuesStream(t)) yield List(v)
    case t :: ts =>
      for {
        tv <- valuesStream(t)
        tsvs <- valuesList(ts)
      } yield tv :: tsvs
  }


}
