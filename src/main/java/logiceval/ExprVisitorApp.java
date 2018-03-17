package logiceval;

public class ExprVisitorApp implements ExprVisitor {
    FuncVisitor funcVisitor;
    public ExprVisitorApp(FuncVisitor funcVisitor) {
        this.funcVisitor = funcVisitor;
    }
    @Override
    public void visit(QuantifierExpr quantifierExpr) {
        quantifierExpr.getBody().acceptEval(this);
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
        app.getFunc().accept(funcVisitor);
    }
}
