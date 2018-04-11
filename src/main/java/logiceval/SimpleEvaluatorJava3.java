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
    HashMap<Expr,HashSet<Expr>> predicateToClause = new HashMap<>();
    Map<VarUse,List <HashSet<Expr>>> varUseListMap = new HashMap<>();
    HashMap<Expr,List <HashSet<Expr>>> negatedEqualities = new HashMap<>();
    @Override
    public Object eval(Expr expr, Structure structure) {
        final Context context = new Context(structure, new HashMap<String,Object>());
        //expr = preProcessing(expr, structure);
        expr = CNFTransformer.transform(expr);
        //expr = preProcessing(expr, structure);
        System.out.println("Nach preProcessing: " + expr);
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
            //TODO wende Kapitel 4 an
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
        //System.out.println(context.getLocalVars() + " " + vu.getName());
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

    private Expr preProcessing(Expr expr, Structure structure) {
        Map<Variable, Quantifier> variables = new HashMap<>();
        HashMap<String,App> containsExpr = new HashMap<>();
        // Map von Varuse -> liste von Klauseln in denen Varuse vorkommt
        Map<App,List <HashSet<Expr>>> equalsMap = new HashMap<>();
        //TODO gleiche keys können vorkommen y=2 mehrmals in der formel z.b.

        HashMap<Expr,HashSet<Expr>> containsToClause = new HashMap<>();
        //TODO set in listen, da klauseln gleich sein können? oder zumindest irgendwie aus der formel filtern
        HashSet<Expr> clauses = new HashSet<>();

        Expr preProcExpr = expr;
        while (preProcExpr instanceof QuantifierExpr) {
            variables.put(((QuantifierExpr) preProcExpr).getVariable(), ((QuantifierExpr) preProcExpr).getQuantifier());
            preProcExpr = ((QuantifierExpr) preProcExpr).getBody();
        }
        fillContainsMap(preProcExpr, containsExpr, equalsMap,variables,predicateToClause, containsToClause, clauses,null, false);
        //preProcessEqualityStructures(equalsMap,containsExpr,variables,structure);
        /*System.out.print(containsExpr);
        System.out.println("VARIABLES: " + variables.toString());
        System.out.println("CONTAINSEXPR: " + containsExpr.toString());
        System.out.println("EQUALSMAP: " + equalsMap.toString());
        System.out.println("Predicate to Clause: " + predicateToClause.toString());
        System.out.println("Contains to Clause: " + containsToClause.toString());
        System.out.println("clauses: " + clauses.toString());*/
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
                    //System.out.println("func: " + ((App) e).getFunc() + " Expr: " + e + " 0: " + ((App)e).getArgs().get(0) + " var: " + variable);
                    if (variable.equals(((App)e).getArgs().get(0))) {
                        //System.out.println("contains: " + e + " " + containsToClause.get(e) + ((containsToClause.get(e).contains(e)) && containsToClause.get(e).size() == 1));
                        if (((containsToClause.get(e).contains(e)) && containsToClause.get(e).size() == 1)) {
                            //doExistentialRemoval();
                            //System.out.println("Existenzverbesserung!Neue Struktur für: "+ ((App) e).getArgs().get(1).toString()+ " " + structure.interpretConstant(((App) e).getArgs().get(1).toString(),null ));
                            SetTypeIterable setTypeIterable = new SetTypeIterable((Set<Object>)structure.interpretConstant(((App) e).getArgs().get(1).toString(),null ),((App) e).getArgs().get(1).toString());
                            variable.setTyp(setTypeIterable);
                        }
                    }
                }
            }
            else {
                //doUniversalCheck();
                    if (variables.get(variable).toString().equals("Forall")) {
                        boolean check = true;
                       // System.out.println("Forall Check");
                        Expr e = containsExpr.get(variable.getName());
                       // System.out.println(clauses.size());
                        //System.out.println("" + e + containsToClause);
                        //TODO warum kann e gleich null sein?
                       // System.out.println(variable.getName() + " " + containsExpr);
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
                                    SetTypeIterable setTypeIterable = new SetTypeIterable((Set<Object>) structure.interpretConstant(((App) e).getArgs().get(1).toString(), null), ((App) e).getArgs().get(1).toString());
                                    variable.setTyp(setTypeIterable);
                                }
                            }
                        }
                        //System.out.println(containsToClause.get(e).size());
                    }
            }
        }
        //TODO checke ob die Gleichheit entfernt werden kann
                /*
                1.Schau dir beide Seiten an ->
                    1.1 double construct oder varuse/construct
                        1.1.1 finde alle varUse auf beiden seiten
                        1.1.2 tue für beide seiten:
                            1.1.1.1 checke alle anderen Klauseln und prüfe ob diese so ein VaruUse enthalten
                            1.1.1.2 entscheide dich für eine Seite wenn keine vorkommen
                    1.2 bei const, nur eine Seite anschauen um zu sehen ob es verbesserbar ist
                    1.3 bei map auch nur eine seite
                 2. Wenn verbesserbar -> entferne alle quantoren mit diesen Variablen
                 3. stelle beim auswerten sicher, dass sobald wir anfangen die Klausel auszuwerten die diese Optimierung enthält, dass wir dies abfangen und anders behandeln
                    weil da VarUse vorkommen, deren Variable wir nicht mehr kennen.
                 4. beachte, dass wir uns noch den Fall anschauen müssen fall die gesamte evaluierung false ergibt. (siehe problemverbesserungs)
                 */
        return preProcessEquality(expr, equalsMap);
    }

    private Expr preProcessEquality(Expr expr,Map<App,List <HashSet<Expr>>> equalsMap){
        // Iteriere alle Gleichheitsausdrücke
        System.out.println(equalsMap);
        System.out.println(negatedEqualities);
        for (Expr equality : equalsMap.keySet()){
            // Fallunterscheidungen
            /*
            1. get = varuse
            2. varuse = varuse
            3. constValue = varuse

            4. constValue = constr
            5. varuse = constr
            6. constr = constr
            7. get = constr
             */
            Expr left = ((App) equality).getArgs().get(0);
            Expr right = ((App) equality).getArgs().get(1);
            ExprBooleanVisitor exprVisitor = new ExprBooleanVisitor() {
                // TODO betrachte die einzelnen fälle und rufe methoden auf die dann checken ob diese Seite viabel ist, bei construct nur an varuses interessiert!
                @Override
                public boolean visit(QuantifierExpr quantifierExpr) {
                    return false;
                }

                @Override
                public boolean visit(VarUse varUse) {
                    List<HashSet<Expr>> a = varUseListMap.get(varUse);
                    // If there is only one clause with this varUse, length of a should be 1 and this varUse is viable
                    if (a !=null) {
                        if (a.size() == 1) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean visit(Undef undef) {
                    return false;
                }

                @Override
                public boolean visit(ConstantValue constantValue) {
                    return false;
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
            };
            boolean leftViable = false;
            boolean rightViable = false;
            if (negatedEqualities.get(equality) != null) {
                 leftViable = left.acceptEquality(exprVisitor);
                 rightViable = left.acceptEquality(exprVisitor);
            }
            ExprWrapper exprWrapper = new ExprWrapper(expr);
            if (leftViable && rightViable) {
                if (left instanceof App) {
                    if (right instanceof App) {
                        if (((App) left).getArgs().size() >= ((App) right).getArgs().size()) {
                            left.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                            return exprWrapper.getExpr();
                        }
                        else {
                            right.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                            return exprWrapper.getExpr();
                        }
                    }
                    left.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                    return exprWrapper.getExpr();
                }
                else {
                    right.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                    return exprWrapper.getExpr();
                }
            }
            if (leftViable) {
                left.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                return exprWrapper.getExpr();
            }
            if (rightViable) {
                right.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                return exprWrapper.getExpr();
            }
        }
        System.out.println("keine Verbesserungen für Gleichheit gefunden");
        return expr;
    }

    private void fillContainsMap(Expr expr, HashMap<String,App> containsExpr, Map<App,List<HashSet<Expr>>> equalsMap,  Map<Variable, Quantifier> variables,HashMap<Expr,HashSet<Expr>> predicateToClause, HashMap<Expr,HashSet<Expr>> containsToClause, HashSet<Expr> clauses, Expr clause, boolean not) {
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
                // Speichere in einer Map wo die VarUse vorkommen für die Gleichheitsverbesserung
                if (varUseListMap.get(expr) == null){
                     ArrayList<HashSet<Expr>> a = new ArrayList<HashSet<Expr>>();
                     a.add(e);
                    varUseListMap.put((VarUse)expr,a);
                }else{
                    varUseListMap.get(expr).add(e);
                }
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
                    //equalsClauses.add(e);
                    if (equalsMap.get(expr) == null){
                        List<HashSet<Expr>> a = new ArrayList<>();
                        a.add(e);
                        equalsMap.put((App)expr,a);
                        if (not){
                            List<HashSet<Expr>> b = new ArrayList<>();
                            b.add(e);
                            negatedEqualities.put(expr,b);
                        }
                    }
                    else{
                        equalsMap.get(expr).add(e);
                        if (not){
                            negatedEqualities.get(expr).add(e);
                        }
                    }
                }
                else{
                    System.out.println("Fall dürfte nicht vorkommen in CNF");
                }
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
                        fillContainsMap(e, containsExpr,equalsMap,variables,predicateToClause,containsToClause,clauses,clause,not);
                    }
                }
        }
        else {
            System.out.println("Missing cases: " + expr.getClass());
            //throw new RuntimeException("Missing cases");
        }
    }
}


