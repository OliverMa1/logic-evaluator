package logiceval;

/**
 * Created by Oliver on 14.01.2018.
 */
public class ExprVisitorEval implements ExprVisitor {
    Context context;
    Evaluator evaluator;
    public ExprVisitorEval(Context context, Evaluator evaluator) {
        this.context = context;
        this.evaluator = evaluator;
    }
    @Override
    public void visit(QuantifierExpr quantifierExpr) {

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

    }

}
