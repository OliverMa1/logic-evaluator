package logiceval

import logiceval.PrettyPrintDoc._

/**
  * Prints '''expressions'''
  */
object Printer {
//TODO
/*
  def printExpr(expr: Expr): Doc = null expr match {
    case App(func, args) =>
      func match {
        case Equals() =>
          binaryOperator("=", args)
        case And() =>
          binaryOperator("∧", args)
        case Or() =>
          binaryOperator("∨", args)
        case Implies() =>
          binaryOperator("⟶", args)
        case Not() =>
          "(¬" <> printExpr(args(0)) <> ")"
        case Contains() =>
          binaryOperator("∈", args)
        case Get() =>
          printExpr(args(0)) <> "[" <> printExpr(args(1)) <> "]"
        case CFunc(name) =>
          if (args.isEmpty) {
            name
          } else {
            name <> "(" <> sep(",", args.map(printExpr)) <> ")"
          }
        case Construct(name) =>
          name <> "(" <> sep(",", args.map(printExpr)) <> ")"
      }
    case QuantifierExpr(q, v, body) =>
      val bodyDoc = printExpr(body)
      nested(4, Alternative(
        "(" <> printQuantifier(q) <> printVariable(v) <> "." <+> bodyDoc <> ")",
        () => "(" <> printQuantifier(q) <> printVariable(v) <> "." </> bodyDoc <> ")"
      ))
    case VarUse(name) =>
      name
    case Undef() =>
      "⊥"
    case ConstantValue(value) =>
      value.toString

  }

  private def binaryOperator(op: String, args: List[Expr]): Doc = {
    val leftDoc = printExpr(args(0))
    val rightDoc = printExpr(args(1))
    nested(2, Alternative(
      "(" <> leftDoc <+> op <+> rightDoc <> ")",
      () => "(" <> leftDoc </> op <+> rightDoc <> ")"
    ))
  }

  private def printQuantifier(q: Quantifier): Doc = q match {
    case Exists() => "∃"
    case Forall() => "∀"
  }

  private def printVariable(v: Variable): Doc =
    v.name <> ":" <+> printType(v.typ)

  private def printType(typ: Type): Doc = typ match {
    case SetType(elementType) =>
      "Set[" <> printType(elementType) <> "]"
    case MapType(keyType, valueType) =>
      "Map[" <> printType(keyType) <> ", " <> printType(valueType) <> "]"
    case DataType(name, fields) =>
      name
    case CustomType(name) =>
      name
  }
*/
}
