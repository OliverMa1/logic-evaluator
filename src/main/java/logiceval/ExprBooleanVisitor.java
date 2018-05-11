package logiceval;

interface ExprBooleanVisitor {
    boolean visit(QuantifierExpr quantifierExpr);
    boolean visit(VarUse varUse);
    boolean visit(Undef undef);
    boolean visit(ConstantValue constantValue);
    boolean visit(App app);
}
