package logiceval;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
        final Context context = new Context(structure, new HashMap<String,Object>()); // problem context jedes mal neu :(
        return eval(expr, context);
    }
    private Object eval(Expr expr, Context context) {
        System.out.println(context.getLocalVars().toString());
        if (expr instanceof QuantifierExpr) {
            QuantifierExpr qe = (QuantifierExpr) expr;
            //System.out.println(qe.getBody().toString());
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
            return eval(args.get(0), context) == eval(args.get(1), context);
        }
        else if (f instanceof And) {
            return (Boolean) eval(args.get(0), context)
                    && (Boolean) eval(args.get(1), context);
        }
        else if (f instanceof Or) {
            return (Boolean) eval(args.get(0), context)
                    ||(Boolean) eval(args.get(1), context);
        }
        else if (f instanceof Implies) {
            return (!(Boolean) eval(args.get(0), context))
                    || ((Boolean) eval(args.get(1), context));
        }
        else if (f instanceof Not) {
            return (!(Boolean) eval(args.get(0), context));
        }
        else if (f instanceof Contains) {
            System.out.println("???");
            Object v = eval(args.get(0), context);
            Set<Object> set = (Set<Object>) eval(args.get(1), context);
            System.out.println(v.toString()+ " " + set.toString());
            return set.contains(v);
        }
        else if (f instanceof Get) {
            Map<Object,Object> datatypeVal =  (Map<Object,Object>)eval(args.get(0), context);
            Object key = eval(args.get(1),context);
            Object r = datatypeVal.get(key);
            if (r == null) {
                return new UndefinedValue();
            }else { return r;}
        }
        else if (f instanceof CFunc) {
            List<Object> args2 = new ArrayList<Object>();
            for (Expr x : args) {
                args2.add(eval(x, context));
            }
            return context.getStructure().interpretConstant(((CFunc) f).getName(), args2.toArray());
        }
        else if (f instanceof Construct) {
            List<Object> args2 = new ArrayList<Object>();
            for (Expr x : args) {
                args2.add(eval(x,context));
            }
            return new DatatypeValue((((Construct) f).getDatatypeName()), args2);
        }
        else {
            throw new RuntimeException("Missing cases");
        }
    }
    private Object evalQuantifierExpr(QuantifierExpr qe, Context context) {
        Variable v = qe.getVariable();
        //Stream<Object> values = StreamSupport.stream(context.getStructure().values(v.getType()).spliterator(), false);
        //System.out.println(qe.getBody().getClass() + v.getName());
        if (qe.getQuantifier() instanceof Exists) {
            // TODO check, probleme mit CRDT
            for (Object value : context.getStructure().values(v.getType())) {
                System.out.println("loop entered");
                //System.out.println(value.toString());
                if (evalBody(value,qe, context, v)) return true;
            }
            //System.out.println("hier");
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
        /*System.out.println("Variable :" + v.getName());
        System.out.println("Body :" + q.getBody().toString());
        System.out.println("old Context: " + context.getLocalVars().toString());*/
        Context newContext = copy(context, context.getStructure());
       // System.out.println("copy Context: " + newContext.getLocalVars().toString());
        newContext.getLocalVars().put(v.getName(),varValue);
      //  System.out.println("new Context: " + newContext.getLocalVars().toString());*/
        System.out.println(q.getBody().getClass());
        Boolean b = (Boolean)eval(q.getBody(), newContext);
       // System.out.println("Erg :" + b);
        return b;//(Boolean)eval(q.getBody(),newContext);
    }

    private Object evalVarUse(VarUse vu, Context context){
        Object a = context.getLocalVars().get(vu.getName());
        if (a == (null)) {
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
           /* try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(context.getLocalVars().get(s));
                oos.flush();
                oos.close();
                bos.close();
                byte[] byteData = bos.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
                Object o1 = (Object) new ObjectInputStream(bais).readObject();
                newContext.getLocalVars().put(s1,o1);
            } catch (Exception e) { e.printStackTrace();}*/
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
