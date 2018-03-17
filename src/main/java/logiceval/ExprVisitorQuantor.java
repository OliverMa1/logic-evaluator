package logiceval;

import java.util.ArrayList;

import static logiceval.JavaDsl.list;

public class ExprVisitorQuantor implements ExprVisitor{
    Expr expr;
    ExprWrapper originalExpr;
    ArrayList<QuantifierExpr> quantifierExprs;
    public ExprVisitorQuantor(Expr expr, ExprWrapper originalExpr, ArrayList<QuantifierExpr> quantifierExprs){
        this.expr = expr;
        this.originalExpr = originalExpr;
        this.quantifierExprs = quantifierExprs;
    }
    @Override
    public void visit(QuantifierExpr quantifierExpr) {
        quantifierExprs.add(quantifierExpr);
        //remove quantifier
        if (expr == null){
            originalExpr.setExpr(quantifierExpr.getBody());
        }
        else if (expr instanceof App) {
            if (((App) expr).getFunc() instanceof And || ((App) expr).getFunc() instanceof Or) {
                //System.out.println("and or " + expr);
                if (((App) expr).getArgs().get(0).equals(quantifierExpr)) {
                    ((App) expr).setArgs(list(quantifierExpr.getBody(), ((App) expr).getArgs().get(1)));
                } else {
                    ((App) expr).setArgs(list(((App) expr).getArgs().get(0), quantifierExpr.getBody()));
                }
            } else if (((App) expr).getFunc() instanceof Not) {
                // System.out.println("not");
                 ((App) expr).setArgs(list(quantifierExpr.getBody()));
            }
        }
        // Negierung ist nach einem Quantor
        else if (expr instanceof QuantifierExpr) {
            System.out.println("hier Quant");
            ((QuantifierExpr) expr).setBody(quantifierExpr.getBody());
        } else {
            // ignore
        }
        // TODO benenne alle variablenvorkommen um von quantifierExpr.getVariable()
        quantifierExpr.getBody().acceptEval(new ExprVisitorQuantor(expr,originalExpr,quantifierExprs));
        //System.out.println(originalExpr);
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
        app.getFunc().accept(new FuncVisitorImplication(app,new ExprVisitorQuantor(app, originalExpr,quantifierExprs)));
    }

}
