package logiceval

import logiceval.AbstractSyntax._

/**
  * Structure for evaluating queries
  */
trait Structure {

  /**
    * returns a lazy stream of all values for the given type
    */
  def values(typ: Type): Stream[Any]

  /**
    * returns the interpretation of a constant
    */
  def interpretConstant(constantName: String): Any



}
