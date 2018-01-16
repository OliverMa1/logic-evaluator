package logiceval;

/**
 * Created by Oliver on 14.01.2018.
 */
interface ExprVisitor {
    void visit(QuantifierExpr quantifierExpr);
    void visit(VarUse varUse);
    void visit(Undef undef);
    void visit(ConstantValue constantValue);
    void visit(App app);
}
