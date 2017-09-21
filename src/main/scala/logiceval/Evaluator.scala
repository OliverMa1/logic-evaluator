package logiceval

import logiceval.AbstractSyntax.Expr

trait Evaluator {

  def eval(expr: Expr, structure: Structure): Any

}
