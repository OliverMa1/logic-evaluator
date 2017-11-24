package logiceval

trait Evaluator {

  def eval(expr: Expr, structure: Structure): Any

}
