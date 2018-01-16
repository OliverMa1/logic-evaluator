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
public class SimpleEvaluatorJava3 implements Evaluator {
    @Override
    public Object eval(Expr expr, Structure structure) {
        final Context context = new Context(structure, new HashMap<String,Object>());
        preProcessing(expr, structure);
        return eval(expr, context);
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
            return ((ConstantValue) expr).getValue();
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
            return eval(args.get(0), context).equals(eval(args.get(1), context));
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
            Object v = eval(args.get(0), context);
            Set<Object> set = (Set<Object>) eval(args.get(1), context);
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
            Object ase = context.getStructure().interpretConstant(((CFunc) f).getName(), args2.toArray());
            return ase;
        }
        else if (f instanceof Construct) {
            List<Object> args2 = new ArrayList<Object>();
            for (Expr x : args) {
                // TODO pair[User1,User2] usw. ist nicht gewollt
                args2.add(eval(x, context));
            }
            //System.out.println(new DatatypeValue((((Construct) f).getDatatypeName()), args2).toString());
            return new DatatypeValue((((Construct) f).getDatatypeName()), args2);
        }
        else {
            throw new RuntimeException("Missing cases");
        }
    }
    private Object evalQuantifierExpr(QuantifierExpr qe, Context context) {
        Variable v = qe.getVariable();
        if (qe.getQuantifier() instanceof Exists) {
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
        Boolean b = (Boolean)eval(q.getBody(), newContext);
        return b;
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

    private void preProcessing(Expr expr, Structure structure) {
        Set<Variable> variables = new HashSet<>();
        Set<App> containsExpr = new HashSet<>();
        Map<Variable, Expr> equalsMap = new HashMap<>();
        Expr preProcExpr = expr;
        while (preProcExpr instanceof QuantifierExpr) {
            variables.add(((QuantifierExpr) preProcExpr).getVariable());
            preProcExpr = ((QuantifierExpr) preProcExpr).getBody();
        }
        fillContainsMap(preProcExpr, containsExpr, equalsMap);
        preProcExpr = expr;
        while (preProcExpr instanceof QuantifierExpr) {
            for(App expr1 : containsExpr) {
                List<Expr> args = expr1.getArgs();
                // TODO wir haben APP cannot be cast to VARUSE, printe mal den typ davon aus :(
                if (args.get(0) instanceof  VarUse) {
                    VarUse varUse = (VarUse) args.get(0);
                    Variable variable =(((QuantifierExpr) preProcExpr).getVariable());
                    if (variable.getName().equals(varUse.getName())) {
                        //((QuantifierExpr) preProcExpr).getVariable()
                        //System.out.println(((QuantifierExpr) preProcExpr).getVariable().getName());
                        SetTypeIterable setTypeIterable = new SetTypeIterable((Set<Object>)structure.interpretConstant(expr1.getArgs().get(1).toString(),null ));
                        variable.setTyp(setTypeIterable);
                    }
                }
                else  if (args.get(0) instanceof  App) {
                    List<Expr> args1 = ((App) args.get(0)).getArgs();
                    System.out.println("args: " + args1 + "func: " + ((App) args.get(0)).getFunc());
                }
            }
            preProcExpr = ((QuantifierExpr) preProcExpr).getBody();
        }
        System.out.println("VARIABLES: " + variables.toString());
        System.out.println("CONTAINSEXPR: " + containsExpr.toString());
        System.out.println("EQUALSMAP: " + equalsMap.toString());

    }

    private void fillContainsMap(Expr expr, Set<App> containsExpr, Map<Variable, Expr> equalsMap) {

        if (expr instanceof VarUse) {

        }
        else if (expr instanceof Undef) {

        }
        else if (expr instanceof ConstantValue) {

        }
        else if (expr instanceof QuantifierExpr) {
            fillContainsMap(((QuantifierExpr) expr).getBody(), containsExpr, equalsMap);
        }
        else if (expr instanceof App) {
            if (((App) expr).getFunc() instanceof Contains) {
                containsExpr.add((App)expr);
            }
            else if (((App) expr).getFunc() instanceof Get) {

            }
            else if (((App) expr).getFunc() instanceof CFunc) {

            }
            else if (((App) expr).getFunc() instanceof Construct) {

            }
            else if (((App) expr).getFunc() instanceof Equals) {
                for (Expr e : ((App) expr).getArgs()) {
                    fillContainsMap(e, containsExpr, equalsMap);
                }
            }
            else {
                for (Expr e : ((App) expr).getArgs()) {
                    fillContainsMap(e, containsExpr, equalsMap);
                }
                fillEqualsMapping(expr, equalsMap);
            }
        }
        else {
            System.out.println(expr.getClass());
            //throw new RuntimeException("Missing cases");
        }
    }
    private void fillEqualsMapping(Expr expr, Map<Variable, Expr> equalsMap) {

    }
}


