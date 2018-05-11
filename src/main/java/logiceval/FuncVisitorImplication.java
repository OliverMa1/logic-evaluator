package logiceval;

import static logiceval.JavaDsl.list;
import static logiceval.JavaDsl.not;

public class FuncVisitorImplication extends FuncVisitorClass{
    public FuncVisitorImplication(App app, ExprVisitor exprVisitor) {
        super(app,exprVisitor);
    }
    @Override
    public void visit(Implies implies){
        Expr left = app.getArgs().get(0);
        Expr right = app.getArgs().get(1);
        left.acceptEval(exprVisitor);
        right.acceptEval(exprVisitor);
        left = not(left);
        app.setArgs(list(left,right));
        app.setFunc(new Or());
    }
    public void visit(And and){
        app.getArgs().get(0).acceptEval(exprVisitor);
        app.getArgs().get(1).acceptEval(exprVisitor);
    }
    public void visit(Or or){
        app.getArgs().get(0).acceptEval(exprVisitor);
        app.getArgs().get(1).acceptEval(exprVisitor);
    }
    public void visit(Not not){
        app.getArgs().get(0).acceptEval(exprVisitor);
    }
}
