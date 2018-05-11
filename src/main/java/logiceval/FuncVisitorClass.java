package logiceval;

public class FuncVisitorClass implements FuncVisitor {
    App app;
    ExprVisitor exprVisitor;
    public FuncVisitorClass(App app, ExprVisitor exprVisitor){
        this.app = app;
        this.exprVisitor = exprVisitor;
    }
    public void visit(Equals equals){}
    public void visit(And and){}
    public void visit(Or or){}
    public void visit(Implies implies){}
    public void visit(Not not){}
    public void visit(Contains contains){}
    public void visit(Get get){}
    public void visit(CFunc cFunc){}
    public void visit(Construct construct){}
}
