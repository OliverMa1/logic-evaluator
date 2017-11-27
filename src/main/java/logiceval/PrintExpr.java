package logiceval;

import java.util.Set;

/**
 * Created by Oliver on 26.11.2017.
 */
public class PrintExpr {
    public static String printExpr(Expr expr) {
        if (expr instanceof App) {
            return handleApp((App) expr);
        }
        else if(expr instanceof QuantifierExpr) {
            return handleQuantifierExpr((QuantifierExpr) expr);
        }
        else if (expr instanceof Undef) {
            return handleUndef();
        }
        else if (expr instanceof VarUse) {
            return handleVarUse((VarUse) expr);
        }
        else if (expr instanceof ConstantValue) {
            return handleConstantValue((ConstantValue) expr);
        }
        else throw new RuntimeException("Fall vergessen");
    }

    private static String  handleApp(App app) {
        String s = "";
        Func func = app.getFunc();
        if (func instanceof Equals) {
            return "(" + printExpr(app.getArgs().get(0)) + " = " + printExpr(app.getArgs().get(1)) +")";
        }
        else if (func instanceof And) {
            return "(" + printExpr(app.getArgs().get(0)) + " ∧ " + printExpr(app.getArgs().get(1)) +")";
        }
        else if (func instanceof Or) {
            return "(" + printExpr(app.getArgs().get(0)) + " ∨ " + printExpr(app.getArgs().get(1)) +")";
        }
        else if (func instanceof Implies) {
            return "(" + printExpr(app.getArgs().get(0)) + " ⟶ " + printExpr(app.getArgs().get(1)) +")";
        }
        else if (func instanceof Not) {
            return "(¬" + printExpr(app.getArgs().get(0)) + ")";
        }
        else if (func instanceof Contains) {
            return "(" + printExpr(app.getArgs().get(0)) + " ∈ " + printExpr(app.getArgs().get(1)) +")";
        }
        else if (func instanceof Get) {
            return "(" + printExpr(app.getArgs().get(0)) + "[" + printExpr(app.getArgs().get(1)) +"])";
        }
        else if (func instanceof CFunc) {
            if (app.getArgs().size() == 0) {
                return ((CFunc) func).getName();
            }
            else {
                s += "(" +((CFunc) func).getName() + "(";
                for (Expr expr : app.getArgs()) {
                    s += printExpr(expr) + ", ";
                }
                s = s.substring(0, s.length()-2);
                s += "))";
                return s;
            }
        }
        else if (func instanceof Construct) {
            s +="(" + ((Construct) func).getDatatypeName() + "(";
            for (Expr expr : app.getArgs()) {
                s += printExpr(expr) + ", ";
            }
            s = s.substring(0, s.length()-2);
            s += "))";
            return s;
        }
        else throw new RuntimeException("Missing cases");
    }

    private static String handleQuantifierExpr(QuantifierExpr quantifierExpr) {
        String s = printExpr(quantifierExpr.getBody());
        if (quantifierExpr.getQuantifier() instanceof Exists) {
            return "(∃" + quantifierExpr.getVariable().getName() + ": " + printType(quantifierExpr.getVariable().getType()) +". " + s + ")";
        }
        else {
            return "(∀" + quantifierExpr.getVariable().getName() + ": " + printType(quantifierExpr.getVariable().getType()) +". " + s + ")";
        }
    }

    private static String handleUndef() {
        return "⊥";
    }

    private static String handleVarUse(VarUse varUse) {
        return varUse.getName();
    }

    private static String handleConstantValue(ConstantValue constantValue) {
        return constantValue.getValue().toString();
    }

    private static String printType(Type type) {
        if (type instanceof SetType) {
            return "Set[" + printType(((SetType) type).getElementType()) +"]";
        }
        else if (type instanceof MapType) {
            return "Map[" + printType(((MapType) type).getKeyType()) + ", " + printType(((MapType) type).getValueType()) + "]";
        }
        else if (type instanceof DataType) {
            return ((DataType) type).getName();
        }
        else if (type instanceof CustomType) {
            return ((CustomType) type).getName();
        }
        else throw new RuntimeException("Missing Cases");
    }
}
