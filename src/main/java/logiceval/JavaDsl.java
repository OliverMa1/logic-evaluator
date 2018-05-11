package logiceval;


import java.util.Arrays;
import java.util.List;

/**
 * Helper methods for building expressions (useful for test-cases)
 */
public class JavaDsl {

    public static Expr forall(Variable v, Expr body) {
        return new QuantifierExpr(new Forall(), v, body);
    }

    public static Expr exists(Variable v, Expr body) {
        return new QuantifierExpr(new Exists(), v, body);
    }

    public static Expr and(Expr... es) {
        Expr result = es[0];
        for (int i = 1; i < es.length; i++) {
            result = new App(new And(), list(result, es[i]));
        }
        return result;
    }

    public static Expr or(Expr... es) {
        Expr result = es[0];
        for (int i = 1; i < es.length; i++) {
            result = new App(new Or(), list(result, es[i]));
        }
        return result;
    }

    public static Expr implies(Expr left, Expr right) {
        return new App(new Implies(), list(left, right));
    }

    public static Expr eq(Expr left, Expr right) {
        return new App(new Equals(), list(left, right));
    }

    public static Expr not(Expr e) {
        return new App(new Not(), list(e));
    }

    public static Expr get(Expr mapExpr, Expr key) {
        return new App(new Get(), list(mapExpr, key));
    }

    public static Expr contains(Expr elem, Expr set) {
        return new App(new Contains(), list(elem, set));
    }

    public static Variable var(String name, Type type) {
        return new Variable(name, type);
    }

    public static CustomType type(String name) {
        return new CustomType(name);
    }

    public static DataType dataType(String name, DataTypeConstructor... contructors) {
        return new DataType(name, list(contructors));
    }

    public static DataTypeConstructor constructor(String name, Type... fields) {
        return new DataTypeConstructor(name, list(fields));
    }


    public static App construct(String name, Expr... fieldValues) {
        return new App(new Construct(name), list(fieldValues));
    }

    public static App pair(Expr c1, Expr c2) {
        return construct("pair", c1, c2);
    }

    public static DatatypeValue pairValue(Object o1, Object o2) {
        return dataTypeValue("pair", o1, o2);
    }
    @SafeVarargs
    public static <T> List<T> list(T... ts) {
        return (Arrays.asList(ts));
    }
/*
    public static <T> List<T> list(java.util.List<T> list) {
        return JavaConversions.asScalaBuffer(list).toList();
    }

    public static <A, B> Map<A, B> map(java.util.Map<A, B> m) {
        return Dsl.javaMapToScala(m);
    }

    public static <V> Set<V> set(java.lang.Iterable<V> set) {
        return Dsl.javaSetToScala(set);
    }

    @SafeVarargs
    public static <V> Set<V> set(V... vals) {
        return set(Arrays.asList(vals));
    }
*/
    public static VarUse varuse(String varname) {
        return new VarUse(varname);
    }

    public static App constantUse(String name) {
        return app(new CFunc(name));
    }

    public static App app(Func func, Expr... args) {
        return new App(func, list(args));
    }

    public static CFunc func(String name) {
        return new CFunc(name);
    }

    public static DatatypeValue dataTypeValue(String name, Object... args) {
        return new DatatypeValue(name, list(args));
    }


}
