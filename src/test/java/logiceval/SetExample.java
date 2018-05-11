package logiceval;


import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static logiceval.JavaDsl.*;
import static org.junit.Assert.assertEquals;


/**
 * This is an example showing the use of sets and a custom finite "int" type
 */
public class SetExample {
    private Evaluator evaluator = new ImprovedEvaluator();
    private Evaluator evaluatorSimple = new SimpleEvaluatorJava();
    private CustomType t_int = type("int");
    private DataType t_pair = dataType("pair", constructor("pair",t_int, t_int));
    private App setA = constantUse("setA");
    private App setB = constantUse("setB");
    private VarUse x = varuse("x");
    private VarUse y = varuse("y");
    private VarUse p = varuse("p");
    private Func lt = func("lt");

    @Test(timeout = 600000)
    public void equalityForall() {
        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 32, 88));
        Structure structure = buildStructure(set1, set2);

        Expr expr = forall(var("p", t_pair),
                forall(var("x", t_int),forall(var("y", t_int),implies(
                        eq(p, pair(x,y)), app(lt,x,y)))));
        long startTime = System.nanoTime();
        Object res = evaluator.eval(expr, structure);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println(duration);
        assertEquals(false, res);

    }
    @Test(timeout = 600000)
    public void test1() {

        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 32, 88));
        Structure structure = buildStructure(set1, set2);

        // (∃x: int. (∃y: int. (((x ∈ setA) ∧ (y ∈ setB)) ∧ (x = y))))
        Expr expr = exists(var("x", t_int),
                exists(var("y", t_int),
                        and(contains(x, setA),
                                contains(y, setB),
                                eq(x, y))));

        //System.out.println(expr);
        long startTime = System.nanoTime();
        Object res = evaluator.eval(expr, structure);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println(duration);
        assertEquals(true, res);
    }
    @Test(timeout = 600000)
    public void cnfTestExists() {
        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 32, 88));
        Structure structure = buildStructure(set1, set2);

        // (∃x: int. (∃y: int. (((((x ∈ setA) ∨ false) ∨ (lt(x, y))) ∧ ((y ∈ setB) ∨ (y = 2))) ∧ (x = y))))
        Expr expr = exists(var("x", t_int),
                exists(var("y", t_int),
                        and(or(((contains(x, setA))), new ConstantValue(false), app(lt,x,y)),
                                or(contains(y, setB),eq(y,new ConstantValue(2))),(not(contains(x,setA))),
                                eq(x, y))));

        System.out.println(expr);
        Object res = evaluator.eval(expr, structure);
        QuantifierExpr a = (QuantifierExpr) expr;
        a = (QuantifierExpr) a.getBody();
        App expr1 = (App)a.getBody();

        assertEquals(true, res);
    }
    @Test(timeout = 600000)
    public void cnfTestExistsSimple() {
        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 32, 88));
        Structure structure = buildStructure(set1, set2);

        // (∃x: int. (∃y: int. (((((x ∈ setA) ∨ false) ∨ (lt(x, y))) ∧ ((y ∈ setB) ∨ (y = 2))) ∧ (x = y))))
        Expr expr = exists(var("x", t_int),
                exists(var("y", t_int),
                        and(or((contains(x, setA)), new ConstantValue(false), app(lt,x,y)),
                                or(contains(y, setB),eq(y,new ConstantValue(2))),(not(contains(x,setA))),
                                eq(x, y))));

        System.out.println(expr);
        Object res = evaluatorSimple.eval(expr, structure);
        QuantifierExpr a = (QuantifierExpr) expr;
        a = (QuantifierExpr) a.getBody();
        App expr1 = (App)a.getBody();

        assertEquals(true, res);
    }
    @Test(timeout = 600000)
    public void cnfTestForall() {
        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 32, 88));
        Structure structure = buildStructure(set1, set2);

        // (∃x: int. (∃y: int. (((((x ∈ setA) ∨ false) ∨ (lt(x, y))) ∧ ((y ∈ setB) ∨ (y = 2))) ∧ (x = y))))
        Expr expr = forall(var("x", t_int),
                forall(var("y", t_int),
                        and(or(not(contains(x, setA)), new ConstantValue(false), app(lt,x,y)),
                                or(not(contains(x,setA)),contains(y, setB),eq(y,new ConstantValue(2))),(not(contains(x,setA))),
                                or(not(contains(x,setA)),eq(x, y)))));

        System.out.println(expr);
        Object res = evaluator.eval(expr, structure);
        QuantifierExpr a = (QuantifierExpr) expr;
        a = (QuantifierExpr) a.getBody();
        App expr1 = (App)a.getBody();
        assertEquals(false, res);
    }
    @Test(timeout = 600000)
    public void cnfTestForallSimple() {
        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 32, 88));
        Structure structure = buildStructure(set1, set2);

        // (∃x: int. (∃y: int. (((((x ∈ setA) ∨ false) ∨ (lt(x, y))) ∧ ((y ∈ setB) ∨ (y = 2))) ∧ (x = y))))
        Expr expr = forall(var("x", t_int),
                forall(var("y", t_int),
                        and(or(not(contains(x, setA)), new ConstantValue(false), app(lt,x,y)),
                                or(not(contains(x,setA)),contains(y, setB),eq(y,new ConstantValue(2))),(not(contains(x,setA))),
                                or(not(contains(x,setA)),eq(x, y)))));

        Object res = evaluatorSimple.eval(expr, structure);
        assertEquals(false, res);
    }
    @Test(timeout = 600000)
    public void test2() {
        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 33, 88));
        Structure structure = buildStructure(set1, set2);

        // (∃x: int. (∃y: int. (((x ∈ setA) ∧ (y ∈ setB)) ∧ (x = y))))
        Expr expr = exists(var("x", t_int),
                exists(var("y", t_int),
                        and(contains(x, setA),
                                contains(y, setB),
                                eq(x, y))));

        long startTime = System.nanoTime();
        Object res = evaluator.eval(expr, structure);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println(duration);
        assertEquals(false, res);

    }
    @Test(timeout = 600000)
    public void test1Simple() {
        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 32, 88));
        Structure structure = buildStructure(set1, set2);

        // (∃x: int. (∃y: int. (((x ∈ setA) ∧ (y ∈ setB)) ∧ (x = y))))
        Expr expr = exists(var("x", t_int),
                exists(var("y", t_int),
                        and(contains(x, setA),
                                contains(y, setB),
                                eq(x, y))));

        long startTime = System.nanoTime();
        Object res = evaluatorSimple.eval(expr, structure);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println(duration);
        assertEquals(true, res);
    }

    @Test(timeout = 600000)
    public void test2Simple() {
        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 33, 88));
        Structure structure = buildStructure(set1, set2);

        // (∃x: int. (∃y: int. (((x ∈ setA) ∧ (y ∈ setB)) ∧ (x = y))))
        Expr expr = exists(var("x", t_int),
                exists(var("y", t_int),
                        and(contains(x, setA),
                                contains(y, setB),
                                eq(x, y))));

        long startTime = System.nanoTime();
        Object res = evaluatorSimple.eval(expr, structure);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println(duration);
        assertEquals(false, res);

    }
    @Test(timeout = 600000)
    public void dnfTestWithImprovement(){
        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 33, 88));
        Structure structure = buildStructure(set1, set2);
        Expr expr = and(new ConstantValue(false),new ConstantValue(false));
        for (int i = 0; i<5;i++){
            expr = or(expr,and(new ConstantValue(false),new ConstantValue(false)));
        }
        Expr expr1 = exists(var("x", t_int),
                exists(var("y", t_int),
                        and(contains(x, setA),
                                contains(y, setB),
                                eq(x, y))));
        expr = and(expr1,expr);
        System.out.println(expr);
        Object res = evaluator.eval(expr, structure);
        assertEquals(false,res);
    }
    @Test(timeout = 600000)
    public void dnfTestWithImprovementSimple(){
        Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 5, 18, 32, 77, 99));
        Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(4, 7, 22, 23, 33, 88));
        Structure structure = buildStructure(set1, set2);        Expr expr = and(new ConstantValue(false),new ConstantValue(false));
        for (int i = 0; i<5;i++){
            expr = or(expr,and(new ConstantValue(false),new ConstantValue(false)));
        }
        Expr expr1 = exists(var("x", t_int),
                exists(var("y", t_int),
                        and(contains(x, setA),
                                contains(y, setB),
                                eq(x, y))));
        expr = and(expr1,expr);
        System.out.println(expr);
        Object res = evaluatorSimple.eval(expr, structure);
        assertEquals(false,res);
    }


    private Structure buildStructure(Set<Integer> a, Set<Integer> b) {
        return new Structure() {

            @Override
            public List<Object> valuesForCustomType(CustomType typ) {
                if (typ.equals(t_int)) {
                    // return values from 1 to 2000
                    return IntStream.range(1, 1000).<Object>mapToObj(x -> x).collect(Collectors.toList());
                } else {
                    throw new RuntimeException("unknown type: " + typ);
                }
            }

            @Override
            public Object interpretConstant(String f, Object[] args) {
                if (f.equals("setA")) {
                    return a;
                } else if (f.equals("setB")) {
                    return b;
                }
               else  if (f.equals("lt")) {
                    return ((int) args[0]) <= ((int) args[1]);
                }
                throw new RuntimeException("unknown constant " + f);
            }

        };
    }

}
