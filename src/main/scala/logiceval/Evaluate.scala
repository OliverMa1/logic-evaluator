package logiceval

import logiceval.AbstractSyntax._

import scala.collection.immutable.Seq

object Evaluate {

  case class Context(
    structure: Structure,
    localVars: Map[String, Any]
  )


  def eval(expr: Expr, structure: Structure): Any = {
    val context = Context(
      structure = structure,
      localVars = Map()
    )
    eval(expr)(context)
  }

  private def eval(expr: Expr)(implicit context: Context): Any = expr match {
    case a: App =>
      evalApp(a)
    case q: QuantifierExpr =>
      evalQuantifierExpr(q)
    case gf: GetField =>
      evalGetFieldExpr(gf)
    case vu: Varuse =>
      evalVarUse(vu)
  }

  private def evalApp(app: App)(implicit context: Context): Any = {
    val args: Seq[Expr] = app.args

    app.func match {
      case Equals() =>
        eval(args(0)) == eval(args(1))
      case And() =>
        eval(args(0)).asInstanceOf[Boolean] && eval(args(1)).asInstanceOf[Boolean]
      case Or() =>
        eval(args(0)).asInstanceOf[Boolean] || eval(args(1)).asInstanceOf[Boolean]
      case Implies() =>
        !eval(args(0)).asInstanceOf[Boolean] || eval(args(1)).asInstanceOf[Boolean]
      case Not() =>
        !eval(args(0)).asInstanceOf[Boolean]
      case Contains() =>
        val v = eval(args(0))
        val set = eval(args(1)).asInstanceOf[Set[Any]]
        set.contains(v)
      case Get() =>
        val datatypeVal = eval(args(0)).asInstanceOf[Map[Any, Any]]
        val key = eval(args(1))
        datatypeVal.getOrElse(key, UndefinedValue())
    }
  }

  private def evalQuantifierExpr(q: QuantifierExpr)(implicit context: Context): Any = {
    val v = q.variable

    val values: Stream[Any] = context.structure.values(v.typ)

    def evalBody(varValue: Any): Boolean = {
      val newContext = context.copy(localVars = context.localVars + (v.name -> varValue))
      eval(q.body)(newContext).asInstanceOf[Boolean]
    }

    q.quantifier match {
      case Exists() =>
        values.exists(evalBody)
      case Forall() =>
        values.forall(evalBody)
    }
  }

  private def evalGetFieldExpr(gf: GetField)(implicit context: Context): Any = {
    val datatypeValue: DatatypeValue = eval(gf.expr).asInstanceOf[DatatypeValue]
    datatypeValue.values.getOrElse(gf.field, () => throw new RuntimeException(s"field ${gf.field} not found"))
  }

  private def evalVarUse(vu: Varuse)(implicit context: Context): Any = {
    val varName = vu.name
    context.localVars.get(varName) match {
      case Some(value) => value
      case None =>
        // when not found in local variable assignment, it must be a constant
        context.structure.interpretConstant(varName)
    }
  }


}
