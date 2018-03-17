package logiceval;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class CNFTransformer {
    public static Expr transform(Expr expr) {
        return null;
    }
    public static void removeImplication(Expr expr) {
        expr.acceptEval(new ExprVisitor() {
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
                app.getFunc().accept(new FuncVisitorImplication(app,this));
            }
        });
    }
    public static void removeNegation(Expr expr){
        expr.acceptEval(new ExprVisitorNegation(null,expr));
    }
    public static Expr moveQuantorVariables(Expr expr){
        ArrayList<QuantifierExpr> quantifierExprs = new ArrayList<>();
        ExprWrapper exprWrapper = new ExprWrapper(expr);
         expr.acceptEval(new ExprVisitorQuantor(null,exprWrapper, quantifierExprs));
        /*for (QuantifierExpr quantifierExpr : quantifierExprs) {
            System.out.println(quantifierExpr.getVariable());
        }*/
        return exprWrapper.getExpr();
    }
}
