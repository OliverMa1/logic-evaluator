package logiceval;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static logiceval.JavaDsl.not;

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
        Map<Variable, Quantifier> variables = new HashMap<>();
        HashMap<String,App> containsExpr = new HashMap<>();
        Map<Expr, Expr> equalsMap = new HashMap<>();
        HashMap<Expr,HashSet<Expr>> predicateToClause = new HashMap<>();
        HashMap<Expr,HashSet<Expr>> containsToClause = new HashMap<>();
        HashSet<Expr> clauses = new HashSet<>();

        Expr preProcExpr = expr;
        while (preProcExpr instanceof QuantifierExpr) {
            variables.put(((QuantifierExpr) preProcExpr).getVariable(), ((QuantifierExpr) preProcExpr).getQuantifier());
            preProcExpr = ((QuantifierExpr) preProcExpr).getBody();
        }
        fillContainsMap(preProcExpr, containsExpr, equalsMap,variables,predicateToClause, containsToClause, clauses,null, false);
        preProcessEqualityStructures(equalsMap,containsExpr,variables,structure);
        System.out.print(containsExpr);
        System.out.println("VARIABLES: " + variables.toString());
        System.out.println("CONTAINSEXPR: " + containsExpr.toString());
        System.out.println("EQUALSMAP: " + equalsMap.toString());
        System.out.println("Predicate to Clause: " + predicateToClause.toString());
        System.out.println("Contains to Clause: " + containsToClause.toString());
        System.out.println("clauses: " + clauses.toString());
        /* prüfe diese zwei fälle!!!!
        1. existenzfall : in einer klausel steht genau ein containsausdruck, zugehörige variable ist
        mit existenzquantor verbunden
            1.1 gehe contains to clause durch, voraussetzung, finde contains ohne negation
            1.2 checke ob get = getKey
                1.2.1 wenn nein, breche ab
                1.2.2 wenn ja, gehe alle übrigen
        2. universalfall: in jeder klausel steht ein negietier containsausdruck, zugehörige variable ist
        mit universalquantor verbunden

        finde den fall heraus und manipuliere die struktur der formel, sodass wir nur noch über alle
        A gehen, optional als erstes: werfe all diese ausdrücke raus oder setze true?? an deren stelle
        a) werfe alle raus ist schwerer
        b) ersetzen ist einfacher, vielleicht durch konstante true?

        Danach normal ausführen! ist möglich, finde beispiel danach.
         */
        // Existenzquantorfall
        for (Variable variable : variables.keySet()) {
            //System.out.println("variables: " + variables.get(variable).toString());
            if (variables.get(variable).toString().equals("Exists")){
                //doExistentialCheck();
                for (Expr e : containsToClause.keySet()){
                    //System.out.println(((App)e).getArgs().get(0).toString() + " " + variable.toString() + " " + variable.equals(((App)e).getArgs().get(0)));
                    System.out.println("func: " + ((App) e).getFunc() + " Expr: " + e + " 0: " + ((App)e).getArgs().get(0) + " var: " + variable);
                    if (variable.equals(((App)e).getArgs().get(0))) {
                        System.out.println("contains: " + e + " " + containsToClause.get(e) + ((containsToClause.get(e).contains(e)) && containsToClause.get(e).size() == 1));
                        if (((containsToClause.get(e).contains(e)) && containsToClause.get(e).size() == 1)) {
                            //doExistentialRemoval();
                            System.out.println("Existenzverbesserung!Neue Struktur für: "+ ((App) e).getArgs().get(1).toString()+ " " + structure.interpretConstant(((App) e).getArgs().get(1).toString(),null ));
                            SetTypeIterable setTypeIterable = new SetTypeIterable((Set<Object>)structure.interpretConstant(((App) e).getArgs().get(1).toString(),null ));
                            variable.setTyp(setTypeIterable);
                        }
                    }
                }
            }
            else {
                //doUniversalCheck();
                    if (variables.get(variable).toString().equals("Forall")) {
                        boolean check = true;
                        System.out.println("Forall Check");
                        Expr e = containsExpr.get(variable.getName());
                       // System.out.println(clauses.size());
                        //System.out.println("" + e + containsToClause);
                        //TODO warum kann e gleich null sein?
                        System.out.println(variable.getName() + " " + containsExpr);
                        if (e != null) {
                            if (((App) e).getFunc() instanceof Not) {
                                //TODO equals funktioniert aus irgendeinem grund nicht richtig bei maps
                                int counter = 0;
                                for (Expr a : containsToClause.keySet()) {
                                    if (a.equals(e)) {
                                        counter++;
                                    }
                                }
                                if (counter == clauses.size()) {
                                    e = ((App) e).getArgs().get(0);
                                    System.out.println("Universalverbesserung!Neue Struktur für " + ((App) e).getArgs().get(1).toString() + " " + structure.interpretConstant(((App) e).getArgs().get(1).toString(), null));
                                    SetTypeIterable setTypeIterable = new SetTypeIterable((Set<Object>) structure.interpretConstant(((App) e).getArgs().get(1).toString(), null));
                                    variable.setTyp(setTypeIterable);
                                }
                            }
                        }
                        //System.out.println(containsToClause.get(e).size());
                    }
            }
        }
    }

    private void fillContainsMap(Expr expr, HashMap<String,App> containsExpr, Map<Expr, Expr> equalsMap,  Map<Variable, Quantifier> variables,HashMap<Expr,HashSet<Expr>> predicateToClause, HashMap<Expr,HashSet<Expr>> containsToClause, HashSet<Expr> clauses, Expr clause, boolean not) {
        if (expr instanceof VarUse) {
            // wenn wir in einer klausel sind, füge das als predikate hinzu mit predikate zu klausel
            // gette, das zuerst, wenn null, dann egal, sonst füge dem Set die klausel hinzu
            if (clause != null) {
                HashSet<Expr> e = predicateToClause.get(expr);
                if (e!= null) {
                    if (not){
                        e.add(not(clause));
                    }
                    else e.add(clause);
                }
                else {
                    e = new HashSet<>();
                    if (not){
                        e.add(not(clause));
                    }
                    else e.add(clause);
                }
                predicateToClause.put(expr, e);
            }
            else{
                System.out.println("Fall dürfte nicht vorkommen in CNF");
            }
        }
        else if (expr instanceof Undef) {
            if (clause != null) {
                HashSet<Expr> e = predicateToClause.get(expr);
                if (e!= null) {
                    if (not){
                        e.add(not(clause));
                    }
                    else e.add(clause);
                }
                else {
                    e = new HashSet<>();
                    if (not){
                        e.add(not(clause));
                    }
                    else e.add(clause);
                }
                predicateToClause.put(expr, e);
            }
            else{
                System.out.println("Fall dürfte nicht vorkommen in CNF");
            }
        }
        else if (expr instanceof ConstantValue) {
            if (clause != null) {
                HashSet<Expr> e = predicateToClause.get(expr);
                if (e!= null) {
                    if (not){
                        e.add(not(clause));
                    }
                    else e.add(clause);
                }
                else {
                    e = new HashSet<>();
                    if (not){
                        e.add(not(clause));
                    }
                    else e.add(clause);
                }
                predicateToClause.put(expr, e);
            }
            else{
                System.out.println("Fall dürfte nicht vorkommen in CNF");
            }
        }
        else if (expr instanceof QuantifierExpr) {
            variables.put(((QuantifierExpr) expr).getVariable(), ((QuantifierExpr) expr).getQuantifier());
            fillContainsMap(((QuantifierExpr) expr).getBody(), containsExpr, equalsMap,variables,predicateToClause,containsToClause,clauses,clause,false);
        }
        else if (expr instanceof App) {
            System.out.println(((App) expr).getFunc());
            if (((App) expr).getFunc() instanceof Contains) {
                if (not) {
                    containsExpr.put(((VarUse) ((App) expr).getArgs().get(0)).getName(), (App) not(expr));
                }
                else {
                    if (((App) expr).getArgs().get(0) instanceof VarUse)
                    containsExpr.put(((VarUse) ((App) expr).getArgs().get(0)).getName(), (App)(expr));
                }
                if (clause != null) {
                    HashSet<Expr> e = containsToClause.get(expr);
                    if (e!= null) {
                        e.add(clause);
                    }
                    else {
                        e = new HashSet<>();
                        e.add(clause);
                        if (not) {
                            containsToClause.put(not(expr), e);
                        }
                        else containsToClause.put(expr,e);
                    }
                }
                else{
                    System.out.println("Fall dürfte nicht vorkommen in CNF");
                }
            }
            else if (((App) expr).getFunc() instanceof Get) {
                if (clause != null) {
                    HashSet<Expr> e = predicateToClause.get(expr);
                    if (e!= null) {
                        if (not){
                            e.add(not(clause));
                        }
                        else e.add(clause);
                    }
                    else {
                        e = new HashSet<>();
                        if (not){
                            e.add(not(clause));
                        }
                        else e.add(clause);
                    }
                    predicateToClause.put(expr, e);
                }
                else{
                    System.out.println("Fall dürfte nicht vorkommen in CNF");
                }
            }
            else if (((App) expr).getFunc() instanceof CFunc) {
                if (clause != null) {
                    HashSet<Expr> e = predicateToClause.get(expr);
                    if (e!= null) {
                        if (not){
                            e.add(not(clause));
                        }
                        else e.add(clause);
                    }
                    else {
                        e = new HashSet<>();
                        if (not) {
                            e.add(not(clause));
                        } else e.add(clause);
                    }
                    predicateToClause.put(expr, e);
                }
                else{
                    System.out.println("Fall dürfte nicht vorkommen in CNF");
                }
            }
            else if (((App) expr).getFunc() instanceof Construct) {
                if (clause != null) {
                    HashSet<Expr> e = predicateToClause.get(expr);
                    if (e!= null) {
                        if (not){
                            e.add(not(clause));
                        }
                        else e.add(clause);
                    }
                    else {
                        e = new HashSet<>();
                        if (not){
                            e.add(not(clause));
                        }
                        else e.add(clause);
                    }
                    predicateToClause.put(expr, e);
                }
                else{
                    System.out.println("Fall dürfte nicht vorkommen in CNF");
                }
            }
            else if (((App) expr).getFunc() instanceof Equals) {

                if (clause != null) {
                    HashSet<Expr> e = predicateToClause.get(expr);
                    if (e!= null) {
                        if (not){
                            e.add(not(clause));
                        }
                        else e.add(clause);
                    }
                    else {
                        e = new HashSet<>();
                        if (not){
                            e.add(not(clause));
                        }
                        else e.add(clause);
                    }
                    predicateToClause.put(expr, e);
                }
                else{
                    System.out.println("Fall dürfte nicht vorkommen in CNF");
                }
                fillEqualsMapping((App)expr, equalsMap);
            }
            else if (((App) expr).getFunc() instanceof And) {
                // wenn einer der argument ein and function ist, dann ist es keine clausel..
                // vielleicht vorher checken?? ja, wenn einer der beiden keine and ist, dann muss es eine klausel sein
                //und alles da drin ist dann eine klausel!!
                for (Expr e : ((App) expr).getArgs()) {
                    //System.out.println("Argumente: " + e);
                }
                for (Expr e : ((App) expr).getArgs()) {
                    if (e instanceof App) {
                        if (!(((App) e).getFunc() instanceof And)) {
                            clause = e;
                            clauses.add(clause);
                        }
                    }
                    fillContainsMap(e, containsExpr, equalsMap,variables,predicateToClause,containsToClause,clauses,clause,not);
                }
            }
            else if (((App) expr).getFunc() instanceof Or) {
                for (Expr e : ((App) expr).getArgs()) {
                    fillContainsMap(e, containsExpr, equalsMap,variables,predicateToClause,containsToClause,clauses,clause,not);
                }
            }
            else if (((App) expr).getFunc() instanceof Not) {
                for (Expr e : ((App) expr).getArgs()) {
                    fillContainsMap(e, containsExpr, equalsMap,variables,predicateToClause,containsToClause,clauses,clause,true);
                }
            }

            else {
                    for (Expr e : ((App) expr).getArgs()) {
                        fillContainsMap(e, containsExpr, equalsMap,variables,predicateToClause,containsToClause,clauses,clause,not);
                    }
                }
        }
        else {
            System.out.println("Missing cases: " + expr.getClass());
            //throw new RuntimeException("Missing cases");
        }
    }
    private void fillEqualsMapping(App expr, Map<Expr, Expr> equalsMap) {
        System.out.println("Gleichheit gefunden!");

        ExprVisitor exprVisitor = new ExprVisitor() {
            @Override
            public void visit(QuantifierExpr quantifierExpr) {

            }

            @Override
            public void visit(VarUse varUse) {
                equalsMap.put(varUse,expr);
            }

            @Override
            public void visit(Undef undef) {

            }

            @Override
            public void visit(ConstantValue constantValue) {
                equalsMap.put(constantValue,expr);
            }

            @Override
            public void visit(App app) {
                equalsMap.put(app,expr);
            }
        };
        expr.getArgs().get(0).acceptEval(exprVisitor);
        expr.getArgs().get(1).acceptEval(exprVisitor);
    }
    private void preProcessEqualityStructures(Map<Expr,Expr> equalsMap, Map<String, App> containsExpr,Map<Variable, Quantifier> variables, Structure structure){
        ExprVisitor exprVisitor = new ExprVisitor() {
            @Override
            public void visit(QuantifierExpr quantifierExpr) {

            }

            @Override
            public void visit(VarUse varUse) {
                Expr expr1 = containsExpr.get(varUse.toString());
                if (!(((App) expr1).getFunc() instanceof Not)){
                    System.out.println("inside " + expr1);
                    System.out.println(((App)expr1).getArgs().get(1));
                    SetTypeIterable setTypeIterable = new SetTypeIterable((Set<Object>)structure.interpretConstant(((App)expr1).getArgs().get(1).toString(),null ));
                    System.out.println("neues Set: " + setTypeIterable.getObjectSet());
                }
                else {
                    System.out.println("rejected " + expr1);
                }

            }

            @Override
            public void visit(Undef undef) {

            }

            @Override
            public void visit(ConstantValue constantValue) {

            }

            @Override
            public void visit(App app) {
                app.getFunc().accept(new FuncVisitor() {
                    @Override
                    public void visit(Equals equals) {

                    }

                    @Override
                    public void visit(And and) {

                    }

                    @Override
                    public void visit(Or or) {

                    }

                    @Override
                    public void visit(Implies implies) {

                    }

                    @Override
                    public void visit(Not not) {

                    }

                    @Override
                    public void visit(Contains contains) {

                    }

                    @Override
                    public void visit(Get get) {
                      //  System.out.println("App args: " + app.getArgs().get(0) +" " +  app.getArgs().get(1));
                       // System.out.println("App args Class: " + app.getArgs().get(0).getClass() +" " +  app.getArgs().get(1).getClass());
                        Expr expr1 = app.getArgs().get(0);
                        Expr expr2 = app.getArgs().get(1);
                    }

                    @Override
                    public void visit(CFunc cFunc) {

                    }

                    @Override
                    public void visit(Construct construct) {

                    }
                });
            }
        };
        for (Expr e : equalsMap.values()) {
            //System.out.println("Equality checks: " + e);
            //System.out.println(((App)e).getArgs().get(0)+ " " + ((App) e).getArgs().get(0).getClass());
            //System.out.println(containsExpr);
            //System.out.println(containsExpr.get((((App)e).getArgs().get(1)).toString()));
            //TODO check ob diese Variable nicht noch woanders vorkommt. muss contains entfernen wenn wir es benutzen
            //TODO entferne die quantifizierung falls erfolgreich (hauptgewinn geliegt darin)
            //TODO decide which side to take o.B.d.A. linke seite bis geklärt
            ((App) e).getArgs().get(0).acceptEval(new ExprVisitorEval(equalsMap, containsExpr,variables ,structure));
            //((App)e).getArgs().get(0).acceptEval(exprVisitor);
            //((App)e).getArgs().get(0).acceptEval(exprVisitor);
        }
    }
}


