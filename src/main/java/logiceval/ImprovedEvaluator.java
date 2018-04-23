package logiceval;

import com.sun.org.apache.xpath.internal.operations.Bool;

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
public class ImprovedEvaluator implements Evaluator {
    // Speichere in welchen Klauseln Expr vorkommen
    HashMap<Expr,HashSet<Expr>> predicateToClause = new HashMap<>();
    // Speichere in welchen Klauseln welche Variablen benutzt werden
    Map<VarUse,List <HashSet<Expr>>> varUseListMap = new HashMap<>();
    // Speichere wo negierte Gleichheiten vorkommen
    HashMap<Expr,List <HashSet<Expr>>> negatedEqualities = new HashMap<>();
    // Speichere die Reihenfolge der Quantoren
    List<QuantifierExpr> quantifierExprs = new ArrayList<>();
    // Speichere Verbindung zwischen Variable und Quantor
    Map<Variable, Quantifier> variables = new HashMap<>();
    // Speichere Verbindung zwischen Variablenname und Quantor
    Map<String, Quantifier> variableNames = new HashMap<>();
    // Speichere Verbindung zwischen Variable und Variablennamen
    Map<String, Variable> namesToVariables = new HashMap<>();
    // Speichere eine Wegbeschreibung für die Gleichheitsverbesserung
    Map<VarUse,List<Object>> varUseDirections = new HashMap<>();
    // Speichere welche Gleichheiten verbessert wurden
    Map<Expr, Expr> improvedEqualities = new HashMap<>();
    // speichere in welchen Gleichheiten welche Variablenbenutzungen vorkommen
    Map<Expr, Expr> varUseToEqualityMap = new HashMap<>();
    // Speichere die Auswertung einer GleichheitsSeite zu dem Datenwert
    Map<Expr, Object> dataValueToOneSide = new HashMap<>();
    Structure structure;

    @Override
    public Object eval(Expr expr, Structure structure) {
        this.structure = structure;
        final Context context = new Context(structure, new HashMap<>());
        //expr = preProcessing(expr, structure);
        expr = CNFTransformer.transform(expr);
        //expr = preProcessing(expr, structure);
        System.out.println("Nach preProcessing: " + preProcessing(expr, structure));
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
            List<Object> args2 = new ArrayList<>();
            for (Expr x : args) {
                args2.add(eval(x, context));
            }
            Object ase = context.getStructure().interpretConstant(((CFunc) f).getName(), args2.toArray());
            return ase;
        }
        else if (f instanceof Construct) {
            List<Object> args2 = new ArrayList<>();
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
        if (!(q.getBody() instanceof QuantifierExpr)){
            //System.out.println("Imporved eqlasok:" +improvedEqualities);
            dataValueToOneSide = new HashMap<>();
            for (Expr expr1 : improvedEqualities.keySet()) {
                dataValueToOneSide.put( improvedEqualities.get(expr1),eval(expr1, newContext));
                /*System.out.println(expr1);
                System.out.println("Auswertung:" + eval(expr1, newContext));
                System.out.println(dataValueToOneSide);
                System.out.println(varUseToEqualityMap);*/
            }
        }
        Boolean b = (Boolean)eval(q.getBody(), newContext);
        return b;
    }

    private Object evalVarUse(VarUse vu, Context context){
        if (varUseDirections.get(vu) == null) {
            Object a = context.getLocalVars().get(vu.getName());
            //System.out.println(context.getLocalVars() + " " + vu.getName());
            if (a == (null)) {
                throw new RuntimeException("Variable ${vu.name} not found.");
            } else return a;
        }
        else {
            Expr equality = varUseToEqualityMap.get(vu);
            Object evaluation = dataValueToOneSide.get(equality);
            List<Object> direction = varUseDirections.get(vu);
            /*Type typ = namesToVariables.get(vu.getName()).getType();
            System.out.println(context.getStructure().values(typ));*/
            Object result = evaluation;
            if (evaluation instanceof DatatypeValue){
                boolean goDeeper = false;
                for (int i = 0; i < direction.size(); i++){
                    if (((DatatypeValue) evaluation).getName().equals(direction.get(0))){
                        if (direction.get(i) instanceof Boolean){
                            goDeeper = (Boolean) direction.get(i);
                        } else {
                            if (direction.get(i) instanceof Integer) {
                                int j = (int)direction.get(i);
                                result = ((DatatypeValue) result).getValues().get(j);
                            }
                            else {
                                //result = null;//((DatatypeValue) result).getValues().get(j);
                            }
                        }
                    }
                    else if (direction.get(0).equals(false)){
                        result = evaluation;
                    }
                    else{
                        result = new UndefinedValue();
                    }
                }
                if (!checkSets(result,vu)){
                    result = new UndefinedValue();
                }
            }
            else{
                //System.out.println("class here: "  + evaluation.getClass() + "wert: " + evaluation);
                result = evaluation;
            }
            /*System.out.println(vu);
            System.out.println(direction);
            System.out.println("eval: " + evaluation);
            System.out.println("res: " + result);*/
            //System.out.println("VarUse: " + vu + " wurde ersetzt durch " + result + " Ursprünglicher Wert: " + evaluation);
            return result;
        }
    }
    private boolean checkSets(Object result, VarUse vu){
        Variable v = namesToVariables.get(vu.getName());
        //structure.interpretConstant(v.getType().toString(), null);
       // System.out.println(v.getType() + "  " + v);
        if (v.getType() instanceof CustomType){
            //System.out.println(structure.valuesForCustomType((CustomType)v.getType())+ " " + result + structure.valuesForCustomType((CustomType)v.getType()).contains(result));
            return structure.valuesForCustomType((CustomType)v.getType()).contains(result);
        }
        if (v.getType() instanceof DataType){
            //System.out.println(((DataType) v.getType()).getConstructors());
            boolean res = true;
            for (DataTypeConstructor d : ((DataType) v.getType()).getConstructors()){
                if (result instanceof DatatypeValue){
                    if (d.getName().equals(((DatatypeValue) result).getName())){
                        int counter = 0;
                        for (Type t : d.getFields()){
                            res &= checkSets(t,((DatatypeValue) result).getValues().get(counter), v);
                            counter ++;
                        }
                    }
                }
                    //System.out.println(d.getName() + " " + d.getFields()+ " result:  " + result.getClass());
            }
            return res;
        }
        return false;
    }
    private boolean checkSets(Type t, Object result, Variable v){

        if (t instanceof CustomType){
            return structure.valuesForCustomType((CustomType)t).contains(result);
        }
        else if(t instanceof DataType){
            if (!((DataType) t).getName().equals(((DatatypeValue) result).getName())){
                return false;
            }
            boolean res = true;
            for (DataTypeConstructor d : ((DataType) t).getConstructors()){
                int counter = 0;
                for (Type type : d.getFields()){
                    res &= checkSets(type, ((DatatypeValue)result).getValues().get(counter),v);
                    counter++;
                }
            }
            return res;
        }
        else{
            return false;
        }
    }

    private Context copy(Context context, Structure structure) {
        Context newContext = new Context(structure, new HashMap<>());
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
            variableNames.put(((QuantifierExpr) preProcExpr).getVariable().getName(), ((QuantifierExpr) preProcExpr).getQuantifier());
            namesToVariables.put(((QuantifierExpr) preProcExpr).getVariable().getName(),((QuantifierExpr) preProcExpr).getVariable());
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
        preProcessContains(clauses,containsToClause,containsExpr);
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
    private void preProcessContains(HashSet<Expr> clauses, HashMap<Expr,HashSet<Expr>> containsToClause, HashMap<String,App> containsExpr){
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
                            System.out.println("Existenzverbesserung!Neue Struktur für: "+ ((App) e).getArgs().get(0).toString()+ " " + structure.interpretConstant(((App) e).getArgs().get(1).toString(),null ));
                            Object o = structure.interpretConstant(((App) e).getArgs().get(1).toString(),null );
                            if (o instanceof Set<?>){
                                SetTypeIterable setTypeIterable = new SetTypeIterable((Set<?>) o,((App) e).getArgs().get(1).toString());
                                variable.setTyp(setTypeIterable);
                            }
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
                                System.out.println("Universalverbesserung!Neue Struktur für " + ((App) e).getArgs().get(0).toString() + " " + structure.interpretConstant(((App) e).getArgs().get(1).toString(), null));
                                Object o = structure.interpretConstant(((App) e).getArgs().get(1).toString(),null );
                                if (o instanceof Set<?>) {
                                    SetTypeIterable setTypeIterable = new SetTypeIterable((Set<?>) o, ((App) e).getArgs().get(1).toString());
                                    variable.setTyp(setTypeIterable);
                                }
                            }
                        }
                    }
                    //System.out.println(containsToClause.get(e).size());
                }
            }
        }
    }

    private Expr preProcessEquality(Expr expr,Map<App,List <HashSet<Expr>>> equalsMap){
        // Iteriere alle Gleichheitsausdrücke
        // Um auf die Mengen später zuzugreifen und die Datentypen zu vergleichen.
        for (Expr equality : equalsMap.keySet()){
            if (negatedEqualities.get(equality) != null) {
                return checkEqualityForall(expr, equality);
            }
            List<HashSet<Expr>> clauses = equalsMap.get(equality);
            if (clauses == null){
                System.out.println("error");
            }
            else{
                for(HashSet<Expr> exprs : clauses){

                    // Alleinige Klausel mit Gleichheit drinnen
                    if (exprs.size() == 1 && exprs.contains(equality)){
                        return checkEqualityExists(expr,equality);
                    }
                }
            }
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

        }
        System.out.println("keine Verbesserungen für Gleichheit gefunden");
        return expr;
    }
    private Expr checkEqualityForall(Expr expr, Expr equality){
        Expr left = ((App) equality).getArgs().get(0);
        Expr right = ((App) equality).getArgs().get(1);
        ExprBooleanVisitorClass exprVisitorLeft = new ExprBooleanVisitorClass(varUseListMap, variableNames);
        ExprBooleanVisitorClass exprVisitorRight = new ExprBooleanVisitorClass(varUseListMap, variableNames);
        boolean leftViable;
        boolean rightViable;
        leftViable = left.acceptEquality(exprVisitorLeft);
        rightViable = right.acceptEquality(exprVisitorRight);
        // ConstValue ist nur in einem Construct erlaubt
        if (left instanceof ConstantValue){
            leftViable = false;
        }
        if (right instanceof ConstantValue){
            rightViable = false;
        }
        return caseDistinction(expr,left,right,exprVisitorLeft.getVarUses(),exprVisitorRight.getVarUses(),leftViable,rightViable, new Forall(), equality);
    }
    private Expr checkEqualityExists(Expr expr, Expr equality){
        Expr left = ((App) equality).getArgs().get(0);
        Expr right = ((App) equality).getArgs().get(1);
        ExprBooleanVisitorClassExists exprVisitorLeft = new ExprBooleanVisitorClassExists(varUseListMap, variableNames);
        ExprBooleanVisitorClassExists exprVisitorRight = new ExprBooleanVisitorClassExists(varUseListMap, variableNames);
        boolean leftViable;
        boolean rightViable;
        leftViable = left.acceptEquality(exprVisitorLeft);
        rightViable = right.acceptEquality(exprVisitorRight);
        // ConstValue ist nur in einem Construct erlaubt
        if (left instanceof ConstantValue){
            leftViable = false;
        }
        if (right instanceof ConstantValue){
            rightViable = false;
        }
        //System.out.println("Left: " + left + " Right: " + right + " leftviable: " + leftViable + " rightviable: " + rightViable);
        return caseDistinction(expr,left,right,exprVisitorLeft.getVarUses(),exprVisitorRight.getVarUses(),leftViable,rightViable, new Exists(), equality);
    }

    private Expr caseDistinction(Expr expr, Expr left, Expr right, List<VarUse> leftVarUse, List<VarUse> rightVarUse, Boolean leftViable, Boolean rightViable, Quantifier quantifier, Expr equality){
        ExprWrapper exprWrapper = new ExprWrapper(expr);
        if (leftViable && rightViable) {
            if (left instanceof App) {
                if (right instanceof App) {
                    if (((App) left).getArgs().size() >= ((App) right).getArgs().size()) {
                        if (checkOrderingForall(leftVarUse,rightVarUse,quantifier)) {
                            giveDirections(left,leftVarUse,varUseDirections,equality);
                            improvedEqualities.put(right,equality);
                            left.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                            return exprWrapper.getExpr();
                        }
                    }
                    else {
                        if (checkOrderingForall(rightVarUse,leftVarUse,quantifier)) {
                            giveDirections(right,rightVarUse,varUseDirections,equality);
                            improvedEqualities.put(left,equality);
                            right.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                            return exprWrapper.getExpr();
                        }
                    }
                }
                if (checkOrderingForall(leftVarUse,rightVarUse,quantifier)) {
                    giveDirections(left,leftVarUse,varUseDirections,equality);
                    improvedEqualities.put(right,equality);
                    left.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                    return exprWrapper.getExpr();
                }
            }
            else {
                if (checkOrderingForall(rightVarUse,leftVarUse,quantifier)) {
                    giveDirections(right,rightVarUse,varUseDirections,equality);
                    improvedEqualities.put(left,equality);
                    right.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                    return exprWrapper.getExpr();
                }
            }
        }
        if (leftViable) {
            if (checkOrderingForall(leftVarUse,rightVarUse,quantifier)) {
                giveDirections(left,leftVarUse,varUseDirections,equality);
                improvedEqualities.put(right,equality);
                left.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                return exprWrapper.getExpr();
            }
        }
        if (rightViable) {
            if (checkOrderingForall(rightVarUse,leftVarUse,quantifier)) {
                giveDirections(right,rightVarUse,varUseDirections,equality);
                improvedEqualities.put(left,equality);
                right.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                return exprWrapper.getExpr();
            }
        }
        return expr;
    }
    private boolean checkOrderingForall(List<VarUse> first, List<VarUse> second,Quantifier quantifier) {
        int firstQuantifier = findFirstQuantifier(second);
        int lastExistsQuantifier;
        if(quantifier instanceof Forall) {
             lastExistsQuantifier = findLastExistsQuantifier(first);
        }
        else{
             lastExistsQuantifier = findLastForallQuantifier(first);
        }
        if(lastExistsQuantifier >= firstQuantifier){
            return false;
        }
        return true;
    }
    private int findFirstQuantifier(List<VarUse> varUses){
        int result = Integer.MAX_VALUE;
        int counter = 0;
        for (QuantifierExpr quantifierExpr : quantifierExprs){
            for (VarUse varUse : varUses){
                if (quantifierExpr.getVariable().getName().equals(varUse.getName())){
                    if (result >= counter){
                        result = counter;
                    }
                }
            }
            counter++;
        }
        return result;
    }
    private int findLastExistsQuantifier(List<VarUse> varUses){
        int result = Integer.MIN_VALUE;
        int counter = 0;

        for (QuantifierExpr quantifierExpr : quantifierExprs){
            for (VarUse varUse : varUses){
                if (quantifierExpr.getVariable().getName().equals(varUse.getName())){
                    if (quantifierExpr.getQuantifier() instanceof Exists) {
                        if (result <= counter) {
                            result = counter;
                        }
                    }
                }
            }
            counter++;
        }
        return result;
    }
    private int findLastForallQuantifier(List<VarUse> varUses){
        int result = Integer.MIN_VALUE;
        int counter = 0;

        for (QuantifierExpr quantifierExpr : quantifierExprs){
            for (VarUse varUse : varUses){
                if (quantifierExpr.getVariable().getName().equals(varUse.getName())){
                    if (quantifierExpr.getQuantifier() instanceof Forall) {
                        if (result <= counter) {
                            result = counter;
                        }
                    }
                }
            }
            counter++;
        }
        return result;
    }
    private void giveDirections(Expr expr, List<VarUse> varUses, Map<VarUse,List<Object>> result, Expr equality){
        fillVarUseToEqualityMap(equality, varUses);
        List<Object> directions = new ArrayList<>();
        if (expr instanceof VarUse){
            directions.add(false);
            result.put((VarUse)expr,directions);
        }
        else if (expr instanceof App){
            if (((App) expr).getFunc() instanceof Construct){
                for (VarUse varUse : varUses) {
                    directions = directionsForConstruct(expr,varUse);
                    // Name an erster Stelle
                    directions.add(0,((Construct) ((App) expr).getFunc()).getDatatypeName());
                    result.put(varUse,directions);
                }
            }
        }
    }
    private void fillVarUseToEqualityMap(Expr equality, List<VarUse> varUses){
        for (VarUse varUse : varUses){
            varUseToEqualityMap.put(varUse, equality);
        }
    }
    private List<Object> directionsForConstruct(Expr expr, VarUse varUse){
        int counter = 0;
        List<Object> a = new ArrayList<>();
        for (Expr expr1 : ((App) expr).getArgs()) {
                if (expr1 instanceof App){
                    if (((App) expr1).getFunc() instanceof Construct){
                        a = directionsForConstruct(expr1,varUse);
                        a.add(0,true);
                        a.add(0,counter);
                    }
                }
                else if (expr1 instanceof VarUse){
                    if (varUse.equals(expr1)) {
                        a.add(counter);
                        a.add(0, false);
                    }
                }
            counter++;
        }
        return a;
    }

    private void fillContainsMap(Expr expr, HashMap<String,App> containsExpr, Map<App,List<HashSet<Expr>>> equalsMap,  Map<Variable, Quantifier> variables,HashMap<Expr,HashSet<Expr>> predicateToClause, HashMap<Expr,HashSet<Expr>> containsToClause, HashSet<Expr> clauses, Expr clause, boolean not) {
        if (expr instanceof VarUse) {

            // wenn wir in einer klausel sind, füge das als predikate hinzu mit predikate zu klausel
            // gette, das zuerst, wenn null, dann egal, sonst füge dem Set die klausel hinzu
            if (clause == null){
                clause = expr;
            }
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
                     ArrayList<HashSet<Expr>> a = new ArrayList<>();
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
            if (clause == null){
                clause = expr;
            }
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
            if (clause == null){
                clause = expr;
            }
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
            quantifierExprs.add((QuantifierExpr)expr);
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
                if (clause == null){
                    clause = expr;
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
                for (Expr e : ((App) expr).getArgs()) {
                    fillContainsMap(e, containsExpr, equalsMap,variables,predicateToClause,containsToClause,clauses,clause,not);
                }
            }
            else if (((App) expr).getFunc() instanceof Get) {
                if (clause == null){
                    clause = expr;
                }
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
                if (clause == null){
                    clause = expr;
                }
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
            else if (((App) expr).getFunc() instanceof Construct) {
            if (clause == null){
                clause = expr;
            }
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
            else if (((App) expr).getFunc() instanceof Equals) {
                if (clause == null){
                    clause = expr;
                }
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
                for (Expr expr1 : ((App) expr).getArgs()) {
                    fillContainsMap(expr1, containsExpr, equalsMap,variables,predicateToClause,containsToClause,clauses,clause,not);
                }
            }
            else if (((App) expr).getFunc() instanceof And) {
                // wenn einer der argument ein and function ist, dann ist es keine clausel..
                // vielleicht vorher checken?? ja, wenn einer der beiden keine and ist, dann muss es eine klausel sein
                //und alles da drin ist dann eine klausel!!
                for (Expr e : ((App) expr).getArgs()) {
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


