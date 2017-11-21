package logiceval;

/**
 * Created by Oliver on 21.11.2017.
 */
// TODO
public class SimpleEvaluatorJava implements Evaluator{

    @Override
    public Object eval(Expr expr, Structure structure) {
        if (expr instanceof QuantifierExpr) {
            QuantifierExpr qe = (QuantifierExpr) expr;
            throw new RuntimeException("TODO implement quantifier with variable " + qe.getVariable());
        }
        throw new RuntimeException("TODO implement " + expr);
    }
}
