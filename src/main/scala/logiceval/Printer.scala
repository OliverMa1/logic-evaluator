package logiceval

import logiceval.AbstractSyntax._
import logiceval.PrettyPrintDoc._

/**
  * Prints '''expressions'''
  */
object Printer {


   def printExpr(expr: Expr): Doc = expr match {
    case App(func, args) =>
      func match {
        case Equals() =>
          "(" <> printExpr(args(0)) <+> "==" <+> printExpr(args(1)) <> ")"
        case And() =>
          "(" <> printExpr(args(0)) <+> "∧" <+> printExpr(args(1)) <> ")"
        case Or() =>
          "(" <> printExpr(args(0)) <+> "∨" <+> printExpr(args(1)) <> ")"
        case Implies() =>
          "(" <> printExpr(args(0)) <+> "⟶" <+> printExpr(args(1)) <> ")"
        case Not() =>
          "(¬" <> printExpr(args(0)) <> ")"
        case Contains() =>
          "(" <> printExpr(args(0)) <+> "∈" <+> printExpr(args(1)) <> ")"
        case Get() =>
          printExpr(args(0)) <> "[" <> printExpr(args(1)) <> "]"
        case CFunc(name) =>
          name <> "(" <> sep(",", args.map(printExpr)) <> ")"
      }
    case QuantifierExpr(q, v, body) =>
      "(" <> printQuantifier(q) <+> printVariable(v) <+> "." <+> printExpr(body) <> ")"
    case GetField(e, field) =>
      printExpr(e) <> "." <> field
    case VarUse(name) =>
      name
    case ConstantUse(name) =>
       name
    case ConstructDatatype(name, values) =>
      name <> "(" <> sep(",", values.toList.map{case (k,v) => k <+> "=" <+> printExpr(v) }) <> ")"
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

}
