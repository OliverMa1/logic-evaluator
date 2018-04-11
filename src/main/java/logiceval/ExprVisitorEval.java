package logiceval;

import java.util.Map;
import java.util.Set;

/**
 * Created by Oliver on 14.01.2018.
 */
public class ExprVisitorEval implements ExprVisitor {
    Map<Expr,Expr> equalsMap;
    Map<String,App> containsExpr;
    Map<Variable,Quantifier> variables;
    Structure structure;
    ExprVisitorEval exprVisitorEval = this;
    public ExprVisitorEval(Map<Expr,Expr> equalsMap, Map<String, App> containsExpr, Map<Variable, Quantifier> variables, Structure structure) {
        this.equalsMap = equalsMap;
        this.containsExpr = containsExpr;
        this.variables = variables;
        this.structure = structure;
    }
    @Override
    public void visit(QuantifierExpr quantifierExpr) {

    }

    @Override
    public void visit(VarUse varUse) {
        //TODO stimmt gar nicht was du hier
        Expr expr1 = containsExpr.get(varUse.toString());
        if (expr1 !=null) {
            if (!(((App) expr1).getFunc() instanceof Not)) {
                //System.out.println(((App)expr1).getArgs().get(1));
                //TODO du sollst nicht, dass aus containsexpr getten sondern, was die variable tatsächlich durchläuft
                SetTypeIterable setTypeIterable = new SetTypeIterable((Set<Object>) structure.interpretConstant(((App) expr1).getArgs().get(1).toString(), null),((App) expr1).getArgs().get(1).toString());
                System.out.println("Gleichheitverbesserung!Neues Set für " + varUse + " " + setTypeIterable.getObjectSet());
            } else {
                System.out.println("rejected " + expr1);
            }
        }
    }

    @Override
    public void visit(Undef undef) {

    }

    @Override
    public void visit(ConstantValue constantValue) {

    }

    @Override
    public void visit(App app) {
        app.getFunc().accept(new FuncVisitor() {
            @Override
            public void visit(Equals equals) {

            }

            @Override
            public void visit(And and) {

            }

            @Override
            public void visit(Or or) {

            }

            @Override
            public void visit(Implies implies) {

            }

            @Override
            public void visit(Not not) {

            }

            @Override
            public void visit(Contains contains) {

            }

            @Override
            public void visit(Get get) {
               // System.out.println("App args: " + app.getArgs().get(0) +" " +  app.getArgs().get(1));
                //System.out.println("App args Class: " + app.getArgs().get(0).getClass() +" " +  app.getArgs().get(1).getClass());
                Expr expr1 = app.getArgs().get(0);
                Expr expr2 = app.getArgs().get(1);
                expr1.acceptEval(exprVisitorEval);
                expr2.acceptEval(exprVisitorEval);
            }

            @Override
            public void visit(CFunc cFunc) {

            }

            @Override
            public void visit(Construct construct) {

            }
        });
    }

}
