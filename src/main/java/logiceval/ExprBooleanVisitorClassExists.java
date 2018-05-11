package logiceval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ExprBooleanVisitorClassExists implements ExprBooleanVisitor{

        private List<VarUse> varUses;
        private Map<String, Quantifier> variables;

        public ExprBooleanVisitorClassExists(Map<String, Quantifier> variables){
            this.varUses = new ArrayList<>();
            this.variables = variables;
        }
        @Override
        public boolean visit(QuantifierExpr quantifierExpr) {
            return false;
        }

        @Override
        public boolean visit(VarUse varUse) {
            varUses.add(varUse);
            return variables.get(varUse.getName()) instanceof Exists;
        }

        @Override
        public boolean visit(Undef undef) {
            return false;
        }

        @Override
        public boolean visit(ConstantValue constantValue) {
            return true;
        }

        @Override
        public boolean visit(App app) {
            if (app.getFunc() instanceof Construct){
                List<Expr> args = app.getArgs();
                boolean res = true;
                if (args.size() <= 1) {
                    return false;
                }
                for (int i = 0; i < args.size(); i++){
                    // erstes arg ist Name vom Konstruktor
                    if (i != 0){
                        res &= args.get(i).acceptEquality(this);
                    }
                }
                return res;
            }
            return false;
        }
        public List<VarUse> getVarUses(){
            return varUses;
        }
    }



