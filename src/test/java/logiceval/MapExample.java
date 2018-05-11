package logiceval;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static logiceval.JavaDsl.*;
import static org.junit.Assert.assertEquals;

//import scala.collection.immutable.Map;


/**
 * This is an example showing the use of maps and a custom finite "int" type
 */
public class MapExample {

    private Evaluator evaluator = new SimpleEvaluatorJava();

    private CustomType t_int = type("int");

    private App m = constantUse("m");
    private VarUse x = varuse("x");
    private VarUse y = varuse("y");
    private Func lt = func("lt");



    @Test
    public void test1() {

        HashMap<Integer, Integer> mBuilder = new HashMap<>();
        mBuilder.put(1, 2);
        mBuilder.put(2, 4);
        mBuilder.put(3, 7);
        mBuilder.put(4, 1);

        Structure structure = buildStructure((mBuilder));

        // (∀x: int. (∀y: int. ((m[x] = y) ⟶ lt(x,y))))
        Expr expr = forall(var("x", t_int),
                forall(var("y", t_int),
                        implies(
                                eq(get(m, x), y),
                                app(lt, x, y))));

        System.out.println(expr);

        Object res = evaluator.eval(expr, structure);
        assertEquals(false, res);
    }


    @Test
    public void test2() {
        HashMap<Integer, Integer> mBuilder = new HashMap<>();
        mBuilder.put(1, 2);
        mBuilder.put(2, 4);
        mBuilder.put(3, 3);
        mBuilder.put(4, 7);
        mBuilder.put(8, 8);

        Structure structure = buildStructure((mBuilder));

        // (∀x: int. (∀y: int. ((m[x] = y) ⟶ lt(x,y))))
        Expr expr = forall(var("x", t_int),
                forall(var("y", t_int),
                        implies(
                                eq(get(m, x), y),
                                app(lt, x, y))));


        System.out.println(expr);

        Object res = evaluator.eval(expr, structure);
        assertEquals(true, res);
    }

    private Structure buildStructure(Map<Integer, Integer> m) {
        return new Structure() {

            @Override
            public List<Object> valuesForCustomType(CustomType typ) {
                if (typ.equals(t_int)) {
                    // return values from 1 to 2000
                    return  IntStream.range(1, 1000).<Object>mapToObj(x -> x).collect(Collectors.toList());
                } else {
                    throw new RuntimeException("unknown type: " + typ);
                }
            }

            @Override
            public Object interpretConstant(String f, Object[] args) {
                if (f.equals("lt")) {
                    return ((int) args[0]) <= ((int) args[1]);
                } else if (f.equals("m")) {
                    return m;
                }
                throw new RuntimeException("unknown constant " + f);
            }

        };
    }

}
