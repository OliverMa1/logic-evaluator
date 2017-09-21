package logiceval;

import logiceval.AbstractSyntax.*;
import scala.collection.JavaConversions;
import scala.collection.immutable.List;
import scala.collection.immutable.Map;

import java.util.Arrays;

/**
 * Helper methods for building expressions (useful for test-cases)
 */
public class JavaDsl {

    public static Expr forall(Variable v, Expr body) {
        return new AbstractSyntax.QuantifierExpr(new AbstractSyntax.Forall(), v, body);
    }

    public static Expr exists(Variable v, Expr body) {
        return new AbstractSyntax.QuantifierExpr(new AbstractSyntax.Exists(), v, body);
    }

    public static Expr and(Expr... es) {
        Expr result = es[0];
        for (int i = 1; i < es.length; i++) {
            result = new App(new AbstractSyntax.And(), list(result, es[i]));
        }
        return result;
    }

    public static Expr or(Expr... es) {
        Expr result = es[0];
        for (int i = 1; i < es.length; i++) {
            result = new App(new AbstractSyntax.Or(), list(result, es[i]));
        }
        return result;
    }

    public static Expr implies(Expr left, Expr right) {
        return new App(new AbstractSyntax.Implies(), list(left, right));
    }

    public static Expr eq(Expr left, Expr right) {
        return new App(new AbstractSyntax.Equals(), list(left, right));
    }

    public static Expr not(Expr e) {
        return new App(new AbstractSyntax.Not(), list(e));
    }

    public static Expr get(Expr mapExpr, Expr key) {
        return new App(new AbstractSyntax.Get(), list(mapExpr, key));
    }

    public static Expr read(Expr e, int field) {
        return new AbstractSyntax.GetField(e, field);
    }


    public static Variable var(String name, AbstractSyntax.Type type) {
        return new Variable(name, type);
    }

    public static CustomType type(String name) {
        return new CustomType(name);
    }


    @SafeVarargs
    public static <T> List<T> list(T... ts) {
        java.util.List<T> list = Arrays.asList(ts);
        return list(list);
    }

    public static <T> List<T> list(java.util.List<T> list) {
        return JavaConversions.asScalaBuffer(list).toList();
    }


    public static <A, B> Map<A, B> map(java.util.Map<A, B> m) {
        return Dsl.javaMapToScala(m);
    }

    public static AbstractSyntax.VarUse varuse(String varname) {
        return new AbstractSyntax.VarUse(varname);
    }

    public static App constantUse(String name) {
        return app(new AbstractSyntax.CFunc(name));
    }

    public static App app(Func func, Expr... args) {
        return new App(func, list(args));
    }

    public static AbstractSyntax.CFunc func(String name) {
        return new AbstractSyntax.CFunc(name);
    }


}
