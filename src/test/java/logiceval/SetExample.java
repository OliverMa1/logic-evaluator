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
    private Evaluator evaluator = new SimpleEvaluatorJava3();
    private Evaluator evaluator3 = new SimpleEvaluatorJava2();
    private Evaluator evaluatorSimple = new SimpleEvaluatorJava();
    private CustomType t_int = type("int");
    private DataType t_pair = dataType("pair", constructor("placeholder",t_int, t_int));
    private App setA = constantUse("setA");
    private App setB = constantUse("setB");
    private VarUse x = varuse("x");
    private VarUse y = varuse("y");
    private VarUse p = varuse("p");


    @Test
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

        System.out.println(expr);
        Object res = evaluator.eval(expr, structure);
        assertEquals(true, res);
    }

    @Test
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

        System.out.println(expr);
        Object res = evaluator.eval(expr, structure);
        assertEquals(false, res);

    }
    @Test
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

        System.out.println(expr);
        Object res = evaluatorSimple.eval(expr, structure);
        assertEquals(true, res);
    }

    @Test
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

        System.out.println(expr);
        Object res = evaluatorSimple.eval(expr, structure);
        assertEquals(false, res);

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
                throw new RuntimeException("TODO implement " + f);
            }

        };
    }

}
