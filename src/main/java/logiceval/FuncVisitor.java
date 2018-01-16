package logiceval;

/**
 * Created by Oliver on 14.01.2018.
 */
interface FuncVisitor {
    void visit(Equals equals);
    void visit(And and);
    void visit(Or or);
    void visit(Implies implies);
    void visit(Not not);
    void visit(Contains contains);
    void visit(Get get);
    void visit(CFunc cFunc);
    void visit(Construct construct);

}
