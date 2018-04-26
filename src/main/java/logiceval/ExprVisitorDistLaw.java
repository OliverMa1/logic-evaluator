package logiceval;

/**
 * Created by Oliver on 24.04.2018.
 */
public class ExprVisitorDistLaw implements ExprVisitor {
    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    private boolean flag = false;
    ExprVisitorDistLaw(Boolean flag){
        this.flag = flag;
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
        app.getFunc().accept(new FuncVisitorDistLaw(app,this));
    }

}
