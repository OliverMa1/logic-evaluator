package logiceval;

/**
 * Created by Oliver on 10.04.2018.
 */
interface ExprBooleanVisitor {
    boolean visit(QuantifierExpr quantifierExpr);
    boolean visit(VarUse varUse);
    boolean visit(Undef undef);
    boolean visit(ConstantValue constantValue);
    boolean visit(App app);
}
