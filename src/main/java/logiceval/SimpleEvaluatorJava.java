package logiceval;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by Oliver on 21.11.2017.
 */
// TODO
public class SimpleEvaluatorJava implements Evaluator {
    @Override
    public Object eval(Expr expr, Structure structure) {
        final Context context = new Context(structure, new HashMap<String,Object>());
        eval(expr, context);
        throw new RuntimeException("TODO implement " + expr);
    }
    private Object eval(Expr expr, Context context) {
        if (expr instanceof QuantifierExpr) {
            QuantifierExpr qe = (QuantifierExpr) expr;
            return evalQuantifierExpr(qe, context);
        }
        else if (expr instanceof VarUse) {
            return evalVarUse((VarUse)expr, context);
        }
        else if (expr instanceof Undef) {
            return new UndefinedValue();
        }
        else if (expr instanceof ConstantValue) {
            return (ConstantValue) ((ConstantValue) expr).getValue();
        }
        else if (expr instanceof App) {
            return evalApp((App) expr, context);
        }
        else throw new RuntimeException("Missing cases");
    }

    private Object evalApp(App a, Context context) {
        Func f = a.getFunc();
        List<Expr> args = a.getArgs();
        if (f instanceof Equals) {
            return eval(args.get(0), context.getStructure()) == eval(args.get(1), context.getStructure());
        }
        else if (f instanceof And) {
            return (Boolean) eval(args.get(0), context.getStructure())
                    && (Boolean) eval(args.get(1), context.getStructure());
        }
        else if (f instanceof Or) {
            return (Boolean) eval(args.get(0), context.getStructure())
                    ||(Boolean) eval(args.get(1), context.getStructure());
        }
        else if (f instanceof Implies) {
            return (!(Boolean) eval(args.get(0), context.getStructure()))
                    || ((Boolean) eval(args.get(1), context.getStructure()));
        }
        else if (f instanceof Not) {
            return (!(Boolean) eval(args.get(0), context.getStructure()));
        }
        else if (f instanceof Contains) {
            Object v = eval(args.get(0), context.getStructure());
            Set<Object> set = (Set<Object>) eval(args.get(1), context.getStructure());
            return set.contains(v);
        }
        else if (f instanceof Get) {
            Map<Object,Object> datatypeVal =  (Map<Object,Object>)eval(args.get(0), context.getStructure());
            Object key = eval(args.get(1),context.getStructure());
            Object r = datatypeVal.get(key);
            if (r.equals(null)) {
                return new UndefinedValue();
            }else { return r;}
        }
        else if (f instanceof CFunc) {
            List<Object> args2 = new ArrayList<Object>();
            for (Expr x : args) {
                args2.add(eval(x,context.getStructure()));
            }
            return context.getStructure().interpretConstant(((CFunc) f).getName(), args2.toArray());
        }
        else if (f instanceof Construct) {
            List<Object> args2 = new ArrayList<Object>();
            for (Expr x : args) {
                args2.add(eval(x,context.getStructure()));
            }
            return new DatatypeValue((((Construct) f).getDatatypeName()), args2);
        }
        else {
            System.out.println("Fall vergessen");
            return null;
        }
    }
    private Object evalQuantifierExpr(QuantifierExpr qe, Context context) {
        Variable v = qe.getVariable();
        Stream<Object> values = StreamSupport.stream(context.getStructure().values(v.getType()).spliterator(), false);
        if (qe.getQuantifier() instanceof Exists) {
            // TODO check
            for (Object value : context.getStructure().values(v.getType())) {
                if (evalBody(value,qe, context, v)) return true;
            }
            return false;
        }
        else {
            for (Object value : context.getStructure().values(v.getType())) {
                if (!evalBody(value,qe, context, v)) return false;
            }
            return true;
        }

    }

    private Boolean evalBody(Object varValue, QuantifierExpr q, Context context, Variable v) {
        Context newContext = copy(context, context.getStructure());
        newContext.getLocalVars().put(v.getName(),varValue);
        return (Boolean)eval(q.getBody(),newContext);
    }

    private Object evalVarUse(VarUse vu, Context context){
        Object a = context.getLocalVars().get(vu.getName());
        if (a.equals(null)) {
            throw new RuntimeException("Variable ${vu.name} not found.");
        }
        else return a;
    }

    private Context copy(Context context, Structure structure) {
        Context newContext = new Context(structure, new HashMap<String, Object>());
        for (String s : context.getLocalVars().keySet()) {
            String s1 = s;
            // TODO deep or shallow
            Object o1 = context.getLocalVars().get(s);
            newContext.getLocalVars().put(s1,o1);
        }
        return newContext;
    }
}

class Context {

    private Structure structure;
    private Map<String,Object> localVars;
    public Context(Structure structure, Map<String,Object> localVars) {
        this.structure = structure;
        this.localVars = localVars;
    }

    public Structure getStructure() {
        return structure;
    }

    public Map<String, Object> getLocalVars() {
        return localVars;
    }
    public void setStructure(Structure structure) {
        this.structure = structure;
    }
}
