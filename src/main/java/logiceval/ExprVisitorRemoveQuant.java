package logiceval;

/**
 * Created by Oliver on 11.04.2018.
 */
public class ExprVisitorRemoveQuant implements ExprVisitor{
        Expr expr;
        ExprWrapper originalExpr;
        public ExprVisitorRemoveQuant(Expr expr, ExprWrapper originalExpr){
            this.expr = expr;
            this.originalExpr = originalExpr;
        }
        @Override
        public void visit(QuantifierExpr quantifierExpr) {

        }

        @Override
        public void visit(VarUse varUse) {
            // solange wir noch ein Quantifierexp finden, expr = qfexpr, checke ob var entfernt werden muss
            Expr e = originalExpr.getExpr();
            while (e instanceof QuantifierExpr) {
                String variableName = ((QuantifierExpr) e).getVariable().getName();
                if (variableName.equals(varUse.getName())) {
                    if (expr == null) {
                        originalExpr.setExpr(((QuantifierExpr) e).getBody());
                    }
                    else {
                        ((QuantifierExpr) expr).setBody(((QuantifierExpr) e).getBody());
                    }
                }
                expr = e;
                e = ((QuantifierExpr) e).getBody();
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

            if(app.getFunc() instanceof Construct) {
                for (Expr e : app.getArgs()) {
                    e.acceptEval(this);
                }
            }
        }
    }

