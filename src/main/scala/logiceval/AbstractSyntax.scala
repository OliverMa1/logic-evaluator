package logiceval

/**
  * The abstract syntax of the logic
  */
object AbstractSyntax {

  sealed abstract class Expr() {

    override def toString: String = Printer.printExpr(this).prettyStr(100)
  }

  case class App(func: Func, args: List[Expr]) extends Expr

  case class QuantifierExpr(quantifier: Quantifier, variable: Variable, body: Expr) extends Expr

  sealed abstract class Quantifier

  case class Exists() extends Quantifier

  case class Forall() extends Quantifier


  case class GetField(expr: Expr, field: String) extends Expr

  case class Varuse(name: String) extends Expr

  case class ConstructDatatype(name: String, values: Map[String, Any]) extends Expr


  case class Variable(name: String, typ: Type)

  sealed abstract class Func

  case class Equals() extends Func()

  case class And() extends Func()

  case class Or() extends Func()

  case class Implies() extends Func()

  case class Not() extends Func()

  // checks if set contains a value
  case class Contains() extends Func()

  // gets a value from a map
  case class Get() extends Func()


  case class Construct(datatypeName: String)


  sealed abstract class Type

  case class SetType(elementType: Type) extends Type

  case class MapType(keyType: Type, valueType: Type) extends Type

  case class DataType(name: String, fields: Seq[(String, Type)]) extends Type

  case class CustomType(name: String) extends Type

  case class FunctionType(argTypes: Seq[Type], resultType: Type)


  sealed abstract class Value

  case class SimpleValue(value: Any) extends Value

  case class DatatypeValue(name: String, values: Map[String, Any]) extends Value

  case class UndefinedValue() extends Value


}
