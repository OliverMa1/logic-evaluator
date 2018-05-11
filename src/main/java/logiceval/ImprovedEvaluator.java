package logiceval;

import java.util.*;

import static logiceval.JavaDsl.not;



public class ImprovedEvaluator implements Evaluator {
    // Speichere in welchen Klauseln Expr vorkommen
    private HashMap<Expr,HashSet<Expr>> predicateToClause = new HashMap<>();
    // Speichere in welchen Klauseln welche Variablen benutzt werden
    private Map<VarUse,List <HashSet<Expr>>> varUseListMap = new HashMap<>();
    // Speichere wo negierte Gleichheiten vorkommen
    private HashMap<Expr,List <HashSet<Expr>>> negatedEqualities = new HashMap<>();
    // Speichere die Reihenfolge der Quantoren
    private List<QuantifierExpr> quantifierExprs = new ArrayList<>();
    // Speichere Verbindung zwischen Variable und Quantor
    private Map<Variable, Quantifier> variables = new HashMap<>();
    // Speichere Verbindung zwischen Variablenname und Quantor
    private Map<String, Quantifier> variableNames = new HashMap<>();
    // Speichere Verbindung zwischen Variable und Variablennamen
    private Map<String, Variable> namesToVariables = new HashMap<>();
    // Speichere eine Wegbeschreibung für die Gleichheitsverbesserung
    private Map<VarUse,List<Object>> varUseDirections = new HashMap<>();
    // Speichere welche Gleichheiten verbessert wurden
    private Map<Expr, Expr> improvedEqualities = new HashMap<>();
    // speichere in welchen Gleichheiten welche Variablenbenutzungen vorkommen
    private Map<Expr, Expr> varUseToEqualityMap = new HashMap<>();
    // Speichere die Auswertung einer GleichheitsSeite zu dem Datenwert
    private Map<Expr, Object> dataValueToOneSide = new HashMap<>();
    Structure structure;

    @Override
    public Object eval(Expr expr, Structure structure) {
        this.structure = structure;
        final Context context = new Context(structure, new HashMap<>());
        //expr = preProcessing(expr, structure);
        expr = CNFTransformer.transform(expr);
        preProcessing(expr, structure);
        //preProcessing(expr, structure);
        System.out.println("Nach preProcessing: " + expr);
        System.out.println("Verbesserte Gleichheiten: " + improvedEqualities +" Wegbeschreibung: " + varUseDirections);
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
            List<Object> args2 = new ArrayList<>();
            for (Expr x : args) {
                args2.add(eval(x, context));
            }
           return context.getStructure().interpretConstant(((CFunc) f).getName(), args2.toArray());
        }
        else if (f instanceof Construct) {
            List<Object> args2 = new ArrayList<>();
            for (Expr x : args) {
                args2.add(eval(x, context));
            }
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

            }
        }
        return (Boolean)eval(q.getBody(), newContext);
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
                for (int i = 0; i < direction.size(); i++){
                    if (((DatatypeValue) evaluation).getName().equals(direction.get(0))){
                            if (direction.get(i) instanceof Integer) {
                                int j = (int)direction.get(i);
                                if(result instanceof DatatypeValue) {
                                    result = ((DatatypeValue) result).getValues().get(j);
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
                result = evaluation;
            }
            return result;
        }
    }
    private boolean checkSets(Object result, VarUse vu){
        Variable v = namesToVariables.get(vu.getName());
        if (v.getType() instanceof CustomType){
            return structure.valuesForCustomType((CustomType)v.getType()).contains(result);
        }
        if (v.getType() instanceof DataType){
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
            boolean erg = false;
            for (DataTypeConstructor d : ((DataType) t).getConstructors()){
                int counter = 0;
                for (Type type : d.getFields()){
                    res &= checkSets(type, ((DatatypeValue)result).getValues().get(counter),v);
                    counter++;
                }
                erg = erg || res;
            }
            return erg;
        }
        else{
            return false;
        }
    }

    private Context copy(Context context, Structure structure) {
        Context newContext = new Context(structure, new HashMap<>());
        for (String s : context.getLocalVars().keySet()) {
            Object o1 = context.getLocalVars().get(s);
            newContext.getLocalVars().put(s,o1);
        }
        return newContext;
    }

    private Expr preProcessing(Expr expr, Structure structure) {

        HashMap<String,ContainsExprInfo> containsExpr = new HashMap<>();
        Map<App,List <HashSet<Expr>>> equalsMap = new HashMap<>();
        HashMap<ContainsExprInfo,HashSet<Expr>> containsToClause = new HashMap<>();
        HashSet<Expr> clauses = new HashSet<>();

        Expr preProcExpr = expr;
        while (preProcExpr instanceof QuantifierExpr) {
            variables.put(((QuantifierExpr) preProcExpr).getVariable(), ((QuantifierExpr) preProcExpr).getQuantifier());
            variableNames.put(((QuantifierExpr) preProcExpr).getVariable().getName(), ((QuantifierExpr) preProcExpr).getQuantifier());
            namesToVariables.put(((QuantifierExpr) preProcExpr).getVariable().getName(),((QuantifierExpr) preProcExpr).getVariable());
            preProcExpr = ((QuantifierExpr) preProcExpr).getBody();
        }
        fillContainsMap(preProcExpr, containsExpr, equalsMap,variables,predicateToClause, containsToClause, clauses,null, false);
        preProcessContains(clauses,containsToClause,containsExpr);

        return preProcessEquality(expr, equalsMap);
    }
    private void preProcessContains(HashSet<Expr> clauses, HashMap<ContainsExprInfo,HashSet<Expr>> containsToClause, HashMap<String,ContainsExprInfo> containsExpr){
        for (Variable variable : variables.keySet()) {
            if (variables.get(variable) instanceof  Exists){
                doExistentialCheck(variable, containsToClause);
            }
            else {
                doUniversalCheck(variable,  clauses, containsToClause,  containsExpr);

            }
        }
    }
    private void doExistentialCheck(Variable variable,
            HashMap<ContainsExprInfo,HashSet<Expr>> containsToClause){
        for (ContainsExprInfo e : containsToClause.keySet()){
            String varUse = e.getVarName();
            if (variable.getName().equals(varUse)){
                if (containsToClause.get(e).size() == 1) {
                    String setName = e.getSetName();
                    Object o = structure.interpretConstant(setName,null );
                    if (o instanceof Set<?>){
                        SetTypeIterable setTypeIterable =
                                new SetTypeIterable((Set<?>) o,(setName));
                        variable.setTyp(setTypeIterable);
                    }
                }
            }
        }
    }
    private void doUniversalCheck(Variable variable,HashSet<Expr> clauses, HashMap<ContainsExprInfo,HashSet<Expr>> containsToClause, HashMap<String,ContainsExprInfo> containsExpr ){
        if (variables.get(variable) instanceof Forall) {
            ContainsExprInfo e = containsExpr.get(variable.getName());
            if (e != null) {
                if (e.isNot()) {
                    int counter = 0;
                    for (ContainsExprInfo a : containsToClause.keySet()) {
                        if (a.equals(e)) {
                            counter++;
                        }
                    }
                    if (counter == clauses.size()) {
                        Object o = structure.interpretConstant(e.getSetName(),null );
                        if (o instanceof Set<?>) {
                            SetTypeIterable setTypeIterable = new SetTypeIterable((Set<?>) o, (e.getSetName()));
                            variable.setTyp(setTypeIterable);
                        }
                    }
                }
            }
        }
    }

    private Expr preProcessEquality(Expr expr,Map<App,List <HashSet<Expr>>> equalsMap){
        for (Expr equality : equalsMap.keySet()){
            if (negatedEqualities.get(equality) != null) {
                 checkEqualityForall(expr, equality);
            }
            List<HashSet<Expr>> clauses = equalsMap.get(equality);
            if (clauses == null){
                System.out.println("error");
            }
            else{
                for(HashSet<Expr> exprs : clauses){
                    if (exprs.size() == 1 && exprs.contains(equality)){
                         checkEqualityExists(expr,equality);
                    }
                }
            }
        }
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
        ExprBooleanVisitorClassExists exprVisitorLeft = new ExprBooleanVisitorClassExists( variableNames);
        ExprBooleanVisitorClassExists exprVisitorRight = new ExprBooleanVisitorClassExists( variableNames);
        boolean leftViable;
        boolean rightViable;
        leftViable = left.acceptEquality(exprVisitorLeft);
        rightViable = right.acceptEquality(exprVisitorRight);
        if (left instanceof ConstantValue){
            leftViable = false;
        }
        if (right instanceof ConstantValue){
            rightViable = false;
        }
        return caseDistinction(expr,left,right,exprVisitorLeft.getVarUses(),exprVisitorRight.getVarUses(),leftViable,rightViable, new Exists(), equality);
    }

    private Expr caseDistinction(Expr expr, Expr left, Expr right, List<VarUse> leftVarUse, List<VarUse> rightVarUse, Boolean leftViable, Boolean rightViable, Quantifier quantifier, Expr equality){
        ExprWrapper exprWrapper = new ExprWrapper(expr);
        if (leftViable && rightViable) {
            if (left instanceof App) {
                if (right instanceof App) {
                    if (((App) left).getArgs().size() >= ((App) right).getArgs().size()) {
                        if (checkOrdering(leftVarUse,rightVarUse,quantifier)) {
                            giveDirections(left,leftVarUse,varUseDirections,equality);
                            improvedEqualities.put(right,equality);
                            left.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                            return exprWrapper.getExpr();
                        }
                    }
                    else {
                        if (checkOrdering(rightVarUse,leftVarUse,quantifier)) {
                            giveDirections(right,rightVarUse,varUseDirections,equality);
                            improvedEqualities.put(left,equality);
                            right.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                            return exprWrapper.getExpr();
                        }
                    }
                }
                if (checkOrdering(leftVarUse,rightVarUse,quantifier)) {
                    giveDirections(left,leftVarUse,varUseDirections,equality);
                    improvedEqualities.put(right,equality);
                    left.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                    return exprWrapper.getExpr();
                }
            }
            else {
                if (checkOrdering(rightVarUse,leftVarUse,quantifier)) {
                    giveDirections(right,rightVarUse,varUseDirections,equality);
                    improvedEqualities.put(left,equality);
                    right.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                    return exprWrapper.getExpr();
                }
            }
        }
        if (leftViable) {
            if (checkOrdering(leftVarUse,rightVarUse,quantifier)) {
                giveDirections(left,leftVarUse,varUseDirections,equality);
                improvedEqualities.put(right,equality);
                left.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                return exprWrapper.getExpr();
            }
        }
        if (rightViable) {
            if (checkOrdering(rightVarUse,leftVarUse,quantifier)) {
                giveDirections(right,rightVarUse,varUseDirections,equality);
                improvedEqualities.put(left,equality);
                right.acceptEval(new ExprVisitorRemoveQuant(null, exprWrapper));
                return exprWrapper.getExpr();
            }
        }
        return expr;
    }
    private boolean checkOrdering(List<VarUse> first, List<VarUse> second,Quantifier quantifier) {
        int firstQuantifier = findFirstQuantifier(second);
        int lastExistsQuantifier;
        if(quantifier instanceof Forall) {
             lastExistsQuantifier = findLastExistsQuantifier(first);
        }
        else{
             lastExistsQuantifier = findLastForallQuantifier(first);
        }
        return lastExistsQuantifier < firstQuantifier;
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
                        a.add(0,counter);
                    }
                }
                else if (expr1 instanceof VarUse){
                    if (varUse.equals(expr1)) {
                        a.add(counter);
                    }
                }
            counter++;
        }
        return a;
    }

    private void fillContainsMap(Expr expr, HashMap<String,ContainsExprInfo> containsExpr, Map<App,List<HashSet<Expr>>> equalsMap,  Map<Variable, Quantifier> variables,HashMap<Expr,HashSet<Expr>> predicateToClause, HashMap<ContainsExprInfo,HashSet<Expr>> containsToClause, HashSet<Expr> clauses, Expr clause, boolean not) {
        if (expr instanceof VarUse) {

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
                if (varUseListMap.get(expr) == null){
                     ArrayList<HashSet<Expr>> a = new ArrayList<>();
                     a.add(e);
                    varUseListMap.put((VarUse)expr,a);
                }else{
                    varUseListMap.get(expr).add(e);
                }
        }
        else if (expr instanceof Undef) {
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
        else if (expr instanceof ConstantValue) {
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
        else if (expr instanceof QuantifierExpr) {
            variables.put(((QuantifierExpr) expr).getVariable(), ((QuantifierExpr) expr).getQuantifier());
            quantifierExprs.add((QuantifierExpr)expr);
            fillContainsMap(((QuantifierExpr) expr).getBody(), containsExpr, equalsMap,variables,predicateToClause,containsToClause,clauses,clause,false);
        }
        else if (expr instanceof App) {
            if (((App) expr).getFunc() instanceof Contains) {
                if (not) {

                    containsExpr.put(((VarUse) ((App) expr).getArgs().get(0)).getName(), new ContainsExprInfo(((App)expr).getArgs().get(0).toString(),((App)expr).getArgs().get(1).toString(),true));
                }
                else {
                    if (((App) expr).getArgs().get(0) instanceof VarUse)
                    containsExpr.put(((VarUse) ((App) expr).getArgs().get(0)).getName(), new ContainsExprInfo(((App)expr).getArgs().get(0).toString(),((App)expr).getArgs().get(1).toString(),false));
                }
                if (clause == null){
                    clause = expr;
                }
                ContainsExprInfo containsExprInfo;
                if (not){
                    containsExprInfo = new ContainsExprInfo(((App)expr).getArgs().get(0).toString(),((App)expr).getArgs().get(1).toString(),true);
                } else{
                    containsExprInfo = new ContainsExprInfo(((App)expr).getArgs().get(0).toString(),((App)expr).getArgs().get(1).toString(),false);
                }
                    HashSet<Expr> e = containsToClause.get(containsExprInfo);
                    if (e!= null) {
                        e.add(clause);
                    }
                    else {
                        e = new HashSet<>();
                        e.add(clause);
                        containsToClause.put(containsExprInfo, e);
                    }
                for (Expr e1 : ((App) expr).getArgs()) {
                    fillContainsMap(e1, containsExpr, equalsMap,variables,predicateToClause,containsToClause,clauses,clause,not);
                }
            }
            else if (((App) expr).getFunc() instanceof Get) {
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
                    if (equalsMap.get(expr) == null){
                        List<HashSet<Expr>> a = new ArrayList<>();
                        a.add(e);
                        equalsMap.put((App)expr,a);
                    } else equalsMap.get(expr).add(e);
                    if (not){
                        if(negatedEqualities.get(expr) == null){
                            List<HashSet<Expr>> b = new ArrayList<>();
                            b.add(e);
                            negatedEqualities.put(expr,b);
                        }
                        else negatedEqualities.get(expr).add(e);

                    }
                }

                for (Expr expr1 : ((App) expr).getArgs()) {
                    fillContainsMap(expr1, containsExpr, equalsMap,variables,predicateToClause,containsToClause,clauses,clause,not);
                }
            }
            if (expr instanceof App) {
                 if (((App) expr).getFunc() instanceof And) {

                    for (Expr e : ((App) expr).getArgs()) {
                        if (e instanceof App) {
                            if (!(((App) e).getFunc() instanceof And)) {
                                clause = e;
                                clauses.add(clause);
                            }
                        }
                        fillContainsMap(e, containsExpr, equalsMap, variables, predicateToClause, containsToClause, clauses, clause, not);
                    }
                } else if (((App) expr).getFunc() instanceof Or) {
                    for (Expr e : ((App) expr).getArgs()) {
                        fillContainsMap(e, containsExpr, equalsMap, variables, predicateToClause, containsToClause, clauses, clause, not);
                    }
                } else if (((App) expr).getFunc() instanceof Not) {
                    for (Expr e : ((App) expr).getArgs()) {
                        fillContainsMap(e, containsExpr, equalsMap, variables, predicateToClause, containsToClause, clauses, clause, true);
                    }
                } else {
                    for (Expr e : ((App) expr).getArgs()) {
                        fillContainsMap(e, containsExpr, equalsMap, variables, predicateToClause, containsToClause, clauses, clause, not);
                    }
                }
            }
        }
    }

class ContainsExprInfo{
    private String varName;
    private String setName;
    private boolean not;

    public String getVarName() {
        return varName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContainsExprInfo)) return false;

        ContainsExprInfo that = (ContainsExprInfo) o;

        if (not != that.not) return false;
        if (varName != null ? !varName.equals(that.varName) : that.varName != null) return false;
        return setName != null ? setName.equals(that.setName) : that.setName == null;
    }

    @Override
    public int hashCode() {
        int result = varName != null ? varName.hashCode() : 0;
        result = 31 * result + (setName != null ? setName.hashCode() : 0);
        result = 31 * result + (not ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ContainsExprInfo{" +

                "varName='" + varName + '\'' +
                ", setName='" + setName + '\'' +
                ", not=" + not +
                '}';
    }

    public String getSetName() {
        return setName;
    }

    public boolean isNot() {
        return not;
    }

    public ContainsExprInfo(String varName, String setName, boolean not){
        this.not = not;

        this.setName = setName;
        this.varName = varName;
    }
}


