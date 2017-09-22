package logiceval

import logiceval.AbstractSyntax._

import scala.collection.JavaConversions
import scala.collection.immutable.Seq

class SimpleEvaluator extends Evaluator {

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
    case vu: VarUse =>
      evalVarUse(vu)
    case Undef() =>
      UndefinedValue()
    case ConstantValue(c) =>
      c
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
        (!eval(args(0)).asInstanceOf[Boolean]) || eval(args(1)).asInstanceOf[Boolean]
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
      case CFunc(name) =>
        context.structure.interpretConstant(name, args.map(eval).toArray)
      case Construct(name) =>
        DatatypeValue(name, args.map(eval))
    }
  }

  private def evalQuantifierExpr(q: QuantifierExpr)(implicit context: Context): Any = {
    val v = q.variable

    val values: Stream[Any] = JavaConversions.iterableAsScalaIterable(context.structure.values(v.typ)).toStream

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


  private def evalVarUse(vu: VarUse)(implicit context: Context): Any = {
    context.localVars.getOrElse(vu.name, throw new RuntimeException(s"Variable ${vu.name} not found."))
  }


}
