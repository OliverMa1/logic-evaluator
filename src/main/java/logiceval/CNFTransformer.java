package logiceval;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class CNFTransformer {
    public static Expr transform(Expr expr) {
        removeImplication(expr);
        expr = removeNegation(expr);
        expr = moveQuantorVariables(expr);
        distributiveLaw(expr);
        //System.out.println(expr);
        return expr;
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
    public static Expr removeNegation(Expr expr){
        ExprWrapper exprWrapper = new ExprWrapper(expr);
        expr.acceptEval(new ExprVisitorNegation(null,exprWrapper));
        return exprWrapper.getExpr();
    }
    public static Expr moveQuantorVariables(Expr expr){
        ArrayList<QuantifierExpr> quantifierExprs = new ArrayList<>();
        ExprWrapper exprWrapper = new ExprWrapper(expr);
         expr.acceptEval(new ExprVisitorQuantor(null,exprWrapper, quantifierExprs,0));
        for (int i = quantifierExprs.size()-1; i >=0; i--){
            QuantifierExpr quantifierExpr = new QuantifierExpr(quantifierExprs.get(i).getQuantifier(),new Variable("x" + i, quantifierExprs.get(i).getVariable().getType()), exprWrapper.getExpr());
            exprWrapper.setExpr(quantifierExpr);
        }
        return exprWrapper.getExpr();
    }
    public static void distributiveLaw(Expr expr) {
        boolean flag = true;
        while (flag){
            ExprVisitorDistLaw exprVisitorDistLaw = new ExprVisitorDistLaw(false);
            expr.acceptEval(exprVisitorDistLaw);
            flag = exprVisitorDistLaw.isFlag();
        }
    }
}
