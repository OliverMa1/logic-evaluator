package logiceval;

import java.util.ArrayList;

import static logiceval.JavaDsl.list;

public class ExprVisitorQuantor implements ExprVisitor{
    private Expr expr;
    private ExprWrapper originalExpr;
    private ArrayList<QuantifierExpr> quantifierExprs;
    private int counter;
    public ExprVisitorQuantor(Expr expr, ExprWrapper originalExpr, ArrayList<QuantifierExpr> quantifierExprs, int counter){
        this.expr = expr;
        this.originalExpr = originalExpr;
        this.quantifierExprs = quantifierExprs;
        this.counter = counter;
    }
    @Override
    public void visit(QuantifierExpr quantifierExpr) {

        quantifierExprs.add(quantifierExpr);
        if (expr == null){
            originalExpr.setExpr(quantifierExpr.getBody());
        }
        else if (expr instanceof App) {
            if (((App) expr).getFunc() instanceof And || ((App) expr).getFunc() instanceof Or) {

                if (((App) expr).getArgs().get(0).equals(quantifierExpr)) {
                    ((App) expr).setArgs(list(quantifierExpr.getBody(), ((App) expr).getArgs().get(1)));
                } else {
                    ((App) expr).setArgs(list(((App) expr).getArgs().get(0), quantifierExpr.getBody()));
                }
            } else if (((App) expr).getFunc() instanceof Not) {

                 ((App) expr).setArgs(list(quantifierExpr.getBody()));
            }
        }

        else if (expr instanceof QuantifierExpr) {
            ((QuantifierExpr) expr).setBody(quantifierExpr.getBody());
        }
        quantifierExpr.acceptEval(new ExprVisitorRemoveVariable(quantifierExpr.getVariable(),counter));
        quantifierExpr.getBody().acceptEval(new ExprVisitorQuantor(expr,originalExpr,quantifierExprs, counter +1));

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
        app.getFunc().accept(new FuncVisitorImplication(app,new ExprVisitorQuantor(app, originalExpr,quantifierExprs, counter )));
    }


}
