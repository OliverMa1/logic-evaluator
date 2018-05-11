package logiceval;

public class ExprVisitorNegation implements ExprVisitor {
    private Expr expr;
    private ExprWrapper originalExpr;
    public ExprVisitorNegation(Expr expr, ExprWrapper originalExpr){
        this.expr = expr;
        this.originalExpr = originalExpr;
    }
    @Override
    public void visit(QuantifierExpr quantifierExpr) {
        quantifierExpr.getBody().acceptEval(new ExprVisitorNegation(expr, originalExpr));
    }

    @Override
    public void visit(VarUse varUse) {

    }

    @Override
    public void visit(Undef undef) {

    }

    @Override
    public void visit(ConstantValue constantValue) {

    }

    @Override
    public void visit(App app) {

        app.getFunc().accept(new FuncVisitorNot(expr,app, this, originalExpr));
    }
}
