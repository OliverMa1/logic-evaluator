package logiceval;

import logiceval.AbstractSyntax.Expr;

public class BetterEvaluator implements Evaluator {
    @Override
    public Object eval(Expr expr, Structure structure) {
        if (expr instanceof AbstractSyntax.QuantifierExpr) {
            AbstractSyntax.QuantifierExpr qe = (AbstractSyntax.QuantifierExpr) expr;
            throw new RuntimeException("TODO implement quantifier with variable " + qe.variable());
        }
        throw new RuntimeException("TODO implement " + expr);
    }
}
