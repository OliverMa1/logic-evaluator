package logiceval

/**
  * The abstract syntax of the logic
  */
object AbstractSyntax {

  sealed abstract class Expr() {

    override def toString: String = Printer.printExpr(this).prettyStr(100)
  }

  /**
    * Function application.
    *
    * @param func the function to call
    * @param args arguments to the function
    */
  case class App(func: Func, args: List[Expr]) extends Expr

  /** An expression with a quantifier */
  case class QuantifierExpr(quantifier: Quantifier, variable: Variable, body: Expr) extends Expr

  /**
    * returns the value "undefined"
    */
  case class Undef() extends Expr

  /** Use of a bound variable  */
  case class VarUse(name: String) extends Expr

  /** Get the value of a field from a datatype-value. */
  case class GetField(expr: Expr, fieldIndex: Int) extends Expr

  /** returns true, if expr is a datatype with the given name */
  case class DatatypeTypecheck(expr: Expr, name: String) extends Expr

  sealed abstract class Quantifier

  case class Exists() extends Quantifier

  case class Forall() extends Quantifier

  case class Variable(name: String, typ: Type)

  sealed abstract class Func

  /** compares two terms for equality  */
  case class Equals() extends Func()

  /** logical and */
  case class And() extends Func()

  /** logical or */
  case class Or() extends Func()

  /** logical implication */
  case class Implies() extends Func()

  /** logical negation */
  case class Not() extends Func()

  // checks if a set contains a value
  case class Contains() extends Func()

  // gets a value from a map
  case class Get() extends Func()

  /** A function from the structure */
  case class CFunc(name: String) extends Func()

  /** Construct a value of a datatype  */
  case class Construct(datatypeName: String) extends Func()


  sealed abstract class Type

  case class SetType(elementType: Type) extends Type

  case class MapType(keyType: Type, valueType: Type) extends Type

  case class DataType(name: String, fields: Seq[Type]) extends Type

  case class CustomType(name: String) extends Type

  case class FunctionType(argTypes: Seq[Type], resultType: Type)


  sealed abstract class Value

  case class SimpleValue(value: Any) extends Value

  case class DatatypeValue(name: String, values: Seq[Any]) extends Value

  case class UndefinedValue() extends Value


}
