package logiceval;

/**
 * Created by Oliver on 17.03.2018.
 */
public class ExprVisitorRemoveVariable implements ExprVisitor {
    Variable variable;
    int counter;
    ExprVisitorRemoveVariable exprVisitorRemoveVariable;
    public ExprVisitorRemoveVariable(Variable variable, int counter) {
        this.variable = variable;
        this.counter = counter;
        exprVisitorRemoveVariable = this;
    }
    @Override
    public void visit(QuantifierExpr quantifierExpr) {
        if (quantifierExpr.getVariable().equals(variable)){
            Variable variable1 = new Variable("x" + counter, variable.getType());
            quantifierExpr.setVariable(variable1);
        }
        quantifierExpr.getBody().acceptEval(this);
    }

    @Override
    public void visit(VarUse varUse) {
        if (varUse.getName().equals(variable.getName())){
            varUse.setName("x" + counter);
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
                app.getArgs().get(0).acceptEval(exprVisitorRemoveVariable);
                app.getArgs().get(1).acceptEval(exprVisitorRemoveVariable);
            }

            @Override
            public void visit(And and) {
                app.getArgs().get(0).acceptEval(exprVisitorRemoveVariable);
                app.getArgs().get(1).acceptEval(exprVisitorRemoveVariable);
            }

            @Override
            public void visit(Or or) {
                app.getArgs().get(0).acceptEval(exprVisitorRemoveVariable);
                app.getArgs().get(1).acceptEval(exprVisitorRemoveVariable);
            }

            @Override
            public void visit(Implies implies) {
                app.getArgs().get(0).acceptEval(exprVisitorRemoveVariable);
                app.getArgs().get(1).acceptEval(exprVisitorRemoveVariable);
            }

            @Override
            public void visit(Not not) {
                app.getArgs().get(0).acceptEval(exprVisitorRemoveVariable);
            }

            @Override
            public void visit(Contains contains) {
                app.getArgs().get(0).acceptEval(exprVisitorRemoveVariable);
            }

            @Override
            public void visit(Get get) {
                app.getArgs().get(0).acceptEval(exprVisitorRemoveVariable);
                app.getArgs().get(1).acceptEval(exprVisitorRemoveVariable);
            }

            @Override
            public void visit(CFunc cFunc) {
                for (Expr expr : app.getArgs()) {
                    expr.acceptEval(exprVisitorRemoveVariable);
                }
            }

            @Override
            public void visit(Construct construct) {
                for (Expr expr : app.getArgs()) {
                    expr.acceptEval(exprVisitorRemoveVariable);
                }
            }
        });
    }
}
