package logiceval;

import static logiceval.JavaDsl.list;
import static logiceval.JavaDsl.not;
import static logiceval.JavaDsl.or;
/**
 * Created by Oliver on 18.03.2018.
 */
public class FuncVisitorDistLaw extends FuncVisitorClass {
    ExprVisitorDistLaw exprVisitor;
    public FuncVisitorDistLaw(App app, ExprVisitorDistLaw exprVisitor) {
        super(app,exprVisitor);
        this.exprVisitor = exprVisitor;
    }
    @Override
    public void visit(Implies implies){
        app.getArgs().get(0).acceptEval(exprVisitor);
        app.getArgs().get(1).acceptEval(exprVisitor);
    }
    public void visit(And and){
        app.getArgs().get(0).acceptEval(exprVisitor);
        app.getArgs().get(1).acceptEval(exprVisitor);
    }
    public void visit(Or or){
        boolean changed = false;
        Expr left = app.getArgs().get(0);
        Expr right = app.getArgs().get(1);
        if (left instanceof App){
            if (((App) left).getFunc() instanceof And) {
                Expr newLeft = or(((App) left).getArgs().get(0),right);
                Expr newRight = or(((App) left).getArgs().get(1),right);
                app.setArgs(list(newLeft,newRight));
                app.setFunc(new And());
                exprVisitor.setFlag(true);
                changed = true;
            }
        }
        if (right instanceof App && !changed){
            if(((App) right).getFunc() instanceof And) {
                Expr newLeft = or(((App) right).getArgs().get(0), left);
                Expr newRight = or(((App) right).getArgs().get(1), left);
                app.setArgs(list(newLeft, newRight));
                app.setFunc(new And());
                exprVisitor.setFlag(true);
            }
        }
        app.getArgs().get(0).acceptEval(exprVisitor);
        app.getArgs().get(1).acceptEval(exprVisitor);
    }
    public void visit(Not not){
        app.getArgs().get(0).acceptEval(exprVisitor);
    }
}
