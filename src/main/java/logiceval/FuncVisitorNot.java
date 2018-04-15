package logiceval;

import static logiceval.JavaDsl.list;
import static logiceval.JavaDsl.not;

public class FuncVisitorNot extends FuncVisitorClass {
    Expr expr;
    boolean changed = false;
    ExprWrapper originalExpr;
    public FuncVisitorNot(Expr expr, App app, ExprVisitor exprVisitor, ExprWrapper originalExpr) {
     super(app, exprVisitor);
     this.expr = expr;
     this.originalExpr = originalExpr;
    }

    @Override
    public void visit(Not not) {
        // Ziehe die Negierung in den Ausdruck, entferne die vorne stehende Negierung danach
        app.getArgs().get(0).acceptEval(new ExprVisitor() {
            @Override
            public void visit(QuantifierExpr quantifierExpr) {
                if (quantifierExpr.getQuantifier() instanceof Forall) {
                    quantifierExpr.setQuantifier(new Exists());
                }
                else {
                    quantifierExpr.setQuantifier(new Forall());
                }
                Expr newBody = not(quantifierExpr.getBody());
                quantifierExpr.setBody(newBody);
                changed = true;
                quantifierExpr.getBody().acceptEval(new ExprVisitorNegation(app, originalExpr));
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
            public void visit(App app1) {
                if (app1.getFunc() instanceof Or){
                    Expr left = app1.getArgs().get(0);
                    Expr right = app1.getArgs().get(1);
                    left = not(left);
                    right = not(right);
                    app1.setFunc(new And());
                    app1.setArgs(list(left,right));
                    changed = true;
                }
                else if (app1.getFunc() instanceof And){
                    Expr left = app1.getArgs().get(0);
                    Expr right = app1.getArgs().get(1);
                    left = not(left);
                    right = not(right);
                    app1.setFunc(new Or());
                    app1.setArgs(list(left,right));
                    changed = true;
                }
                else if (app1.getFunc() instanceof Not){
                    app.setArgs(list(app1.getArgs().get(0)));
                    changed = true;
                }
                else {
                    //ignore
                }
            }
        });
        // Negierung steht ganz vorne in der Formel
        if (changed) {
            if (expr == null) {
                originalExpr.setExpr(app.getArgs().get(0));
            }
            // Negierung ist in einem Oder,Und, Nicht
            else if (expr instanceof App) {
                if (((App) expr).getFunc() instanceof And || ((App) expr).getFunc() instanceof Or) {
                    if (((App) expr).getArgs().get(0).equals(app)) {
                        ((App) expr).setArgs(list(app.getArgs().get(0), ((App) expr).getArgs().get(1)));
                    } else {
                        ((App) expr).setArgs(list(((App) expr).getArgs().get(0), app.getArgs().get(0)));
                    }
                }
            }
            // Negierung ist nach einem Quantor
            else if (expr instanceof QuantifierExpr) {
                ((QuantifierExpr) expr).setBody(app.getArgs().get(0));
            } else {
                // ignore
            }
            changed = false;
        }
        app.getArgs().get(0).acceptEval(new ExprVisitorNegation(app, originalExpr));
    }

    @Override
    public void visit(Contains contains) {

    }

    @Override
    public void visit(Get get) {

    }

    @Override
    public void visit(CFunc cFunc) {

    }

    @Override
    public void visit(Construct construct) {

    }

    @Override
    public void visit(Or or) {
        app.getArgs().get(0).acceptEval(new ExprVisitorNegation(app, originalExpr));
        app.getArgs().get(1).acceptEval(new ExprVisitorNegation(app, originalExpr));
    }

    @Override
    public void visit(Implies implies) {

    }

    @Override
    public void visit(Equals equals) {

    }

    @Override
    public void visit(And and) {
        app.getArgs().get(0).acceptEval(new ExprVisitorNegation(app, originalExpr));
        app.getArgs().get(1).acceptEval(new ExprVisitorNegation(app, originalExpr));
    }
}
