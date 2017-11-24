package logiceval

import scala.collection.JavaConversions
import scala.language.implicitConversions

/**
  * Embedded domain specific language (DSL) for constructing formulas.
  */
object Dsl {
/*
  implicit def varuse(name: String): VarUse = VarUse(name)

  def forall(vars: Variable*)(body: Expr): Expr = {
    var result = body
    for (v <- vars.reverse) {
      result = QuantifierExpr(Forall(), v, result)
    }
    result
  }

  def exists(vars: Variable*)(body: Expr): Expr = {
    var result = body
    for (v <- vars.reverse) {
      result = QuantifierExpr(Exists(), v, result)
    }
    result
  }


  def !(e: Expr): Expr = App(Not(), List(e))

  implicit class ExprExtensions(x: Expr) {

    def &&&(y: Expr): Expr = App(And(), List(x, y))

    def |||(y: Expr): Expr = App(Or(), List(x, y))

    def ==>(y: Expr): Expr = App(Implies(), List(x, y))

    def =:=(y: Expr): Expr = App(Equals(), List(x, y))

    def in(y: Expr): Expr = App(Contains(), List(x, y))
  }

  implicit class TypeExtensions(t: Type) {

    def ::(name: String): Variable = Variable(name, t)

  }

  def javaMapToScala[K, V](m: java.util.Map[K, V]): Map[K, V] = {
    JavaConversions.mapAsScalaMap(m).toMap
  }

  def javaSetToScala[V](it: java.lang.Iterable[V]): Set[V] = {
    JavaConversions.iterableAsScalaIterable(it).toSet
  }

*/
}
