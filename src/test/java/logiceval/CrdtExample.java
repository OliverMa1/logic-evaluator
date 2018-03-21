package logiceval;

import logiceval.AbstractSyntax.*;
import org.junit.Test;
//import scala.collection.immutable.Set;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static logiceval.JavaDsl.*;
import static org.junit.Assert.assertEquals;


/**
 * This is an example modeling CRDTs similar to the userbase example
 * (see https://github.com/peterzeller/repliss/blob/1bf75c9c09f4dc412db4a7410d1c4662c2a73070/src/main/resources/examples/userbase.rpls )
 */
public class CrdtExample {

    private Evaluator evaluatorSimple = new SimpleEvaluatorJava();
    private Evaluator evaluator2 = new SimpleEvaluatorJava2();
    private Evaluator evaluatorImproved = new SimpleEvaluatorJava3();
    private CustomType t_String = type("string");
    private CustomType t_userId = type("userId");
    private CustomType t_callId = type("callId");
    private DataType t_userRecordField = dataType("userRecordField",
            constructor("F_name"),
            constructor("F_mail")
    );
    private DataType t_pair = dataType("pair", constructor("pair", t_callId, t_callId));
    private DataType t_UserPair = dataType("pair", constructor("pair", t_userId, t_userId));
    private DataType t_callInfo = dataType("callInfo",
            constructor("mapWrite", t_userId, t_userRecordField, t_String),
            constructor("mapDelete", t_userId)
    );

    private App c_visibleCalls = constantUse("visibleCalls");
    private App c_op = constantUse("op");
    private App c_happensBefore = constantUse("happensBefore");
    private App c_user = constantUse("user");
    private VarUse x = varuse("x");
    private VarUse p = varuse("p");


    @Test
    public void test1Simple() {
        Set<Integer> visibleCalls = new HashSet<>();
        visibleCalls.add(1);
        visibleCalls.add(2);
        visibleCalls.add(3);
        HashMap<Integer, DatatypeValue> callOps = new LinkedHashMap<>();
        callOps.put(1, dataTypeValue("mapWrite", "User1", dataTypeValue("F_name"), "String42"));
        callOps.put(2, dataTypeValue("mapWrite", "User2", dataTypeValue("F_mail"), "String12"));
        callOps.put(3, dataTypeValue("mapDelete", "User1"));
        Set<DatatypeValue> happensBefore = new HashSet<>();
        happensBefore.add(pairValue(1,2));
        happensBefore.add(pairValue(2,3));
        String user = "User1";
        Structure structure = buildStructure(visibleCalls, callOps, happensBefore, user);
        Expr expr = mapExistsQuery();
        expr = CNFTransformer.transform(expr);
        Object res = evaluatorSimple.eval(expr, structure);
        assertEquals(false, res);
    }

    @Test
    public void test1() {
        Set<Integer> visibleCalls = new HashSet<>();
        visibleCalls.add(1);
        visibleCalls.add(2);
        visibleCalls.add(3);
        HashMap<Integer, DatatypeValue> callOps = new LinkedHashMap<>();
        callOps.put(1, dataTypeValue("mapWrite", "User1", dataTypeValue("F_name"), "String42"));
        callOps.put(2, dataTypeValue("mapWrite", "User2", dataTypeValue("F_mail"), "String12"));
        callOps.put(3, dataTypeValue("mapDelete", "User1"));
        Set<DatatypeValue> happensBefore = new HashSet<>();
        happensBefore.add(pairValue(1,2));
        happensBefore.add(pairValue(2,3));
        String user = "User1";
        Structure structure = buildStructure(visibleCalls, callOps, happensBefore, user);

        Expr expr = mapExistsQuery();
        System.out.println(expr);
        CNFTransformer.removeImplication(expr);
        System.out.println(expr);
        CNFTransformer.removeNegation(expr);
        System.out.println(expr);
        expr = CNFTransformer.moveQuantorVariables(expr);
        System.out.println(expr);
        CNFTransformer.distributiveLaw(expr);
        System.out.println(expr);
        Object res = evaluatorImproved.eval(expr, structure);
        assertEquals(false, res);
    }

    @Test
    public void test2Simple() {
        Set<Integer> visibleCalls = new HashSet<>();
        visibleCalls.add(1);
        visibleCalls.add(2);
        visibleCalls.add(3);
        HashMap<Integer, DatatypeValue> callOps = new LinkedHashMap<>();
        callOps.put(1, dataTypeValue("mapWrite", "User1", dataTypeValue("F_name"), "String42"));
        callOps.put(2, dataTypeValue("mapWrite", "User2", dataTypeValue("F_mail"), "String12"));
        callOps.put(3, dataTypeValue("mapDelete", "User1"));
        Set<DatatypeValue> happensBefore = new HashSet<>();
        happensBefore.add(pairValue(2,3));
        String user = "User1";
        Structure structure = buildStructure(visibleCalls, callOps, happensBefore, user);

        Expr expr = mapExistsQuery();
        System.out.println(expr);

        Object res = evaluatorSimple.eval(expr, structure);
        assertEquals(false, res);
    }
    @Test
    public void test2() {
        Set<Integer> visibleCalls = new HashSet<>();
        visibleCalls.add(1);
        visibleCalls.add(2);
        visibleCalls.add(3);
        HashMap<Integer, DatatypeValue> callOps = new LinkedHashMap<>();
        callOps.put(1, dataTypeValue("mapWrite", "User1", dataTypeValue("F_name"), "String42"));
        callOps.put(2, dataTypeValue("mapWrite", "User2", dataTypeValue("F_mail"), "String12"));
        callOps.put(3, dataTypeValue("mapDelete", "User1"));
        Set<DatatypeValue> happensBefore = new HashSet<>();
        happensBefore.add(pairValue(2,3));
        String user = "User1";
        Structure structure = buildStructure(visibleCalls, callOps, happensBefore, user);

        Expr expr = mapExistsQuery();
        System.out.println(expr);

        Object res = evaluatorImproved.eval(expr, structure);
        assertEquals(false, res);
    }

    @Test
    public void dataTypeTestSimple() {
        Set<Integer> visibleCalls = new HashSet<>();
        visibleCalls.add(1);
        visibleCalls.add(2);
        visibleCalls.add(3);
        HashMap<Integer, DatatypeValue> callOps = new LinkedHashMap<>();
        callOps.put(1, dataTypeValue("mapWrite", "User1", dataTypeValue("F_name"), "String1"));
        callOps.put(2, dataTypeValue("mapWrite", "User2", dataTypeValue("F_mail"), "String2"));
        callOps.put(3, dataTypeValue("mapDelete", "User2"));
        Set<DatatypeValue> happensBefore = new HashSet<>();
        happensBefore.add(pairValue(2,3));
        happensBefore.add(pairValue(3,1));
        String user = "User1";
        Structure structure = buildStructure(visibleCalls, callOps, happensBefore, user);

        Expr expr = mapExistsQuery();
        System.out.println("\n Print ende \n");
        expr = forall(var("x", t_userId),
                exists(var("p", t_UserPair),
                        eq(p, pair(x,x))));
        Object res = evaluatorSimple.eval(expr, structure);
        /*for (Object o : structure.values(t_callInfo)) {
            System.out.println(o.toString());
        }
        for (Object o : structure.values(t_UserPair)) {
            System.out.println(o.toString());
        }*/
        assertEquals(true, res);

    }
    @Test
    public void dataTypeTest() {
        Set<Integer> visibleCalls = new HashSet<>();
        visibleCalls.add(1);
        visibleCalls.add(2);
        visibleCalls.add(3);
        HashMap<Integer, DatatypeValue> callOps = new LinkedHashMap<>();
        callOps.put(1, dataTypeValue("mapWrite", "User1", dataTypeValue("F_name"), "String1"));
        callOps.put(2, dataTypeValue("mapWrite", "User2", dataTypeValue("F_mail"), "String2"));
        callOps.put(3, dataTypeValue("mapDelete", "User2"));
        Set<DatatypeValue> happensBefore = new HashSet<>();
        happensBefore.add(pairValue(2,3));
        happensBefore.add(pairValue(3,1));
        String user = "User1";
        Structure structure = buildStructure(visibleCalls, callOps, happensBefore, user);

        Expr expr = mapExistsQuery();
        System.out.println("\n Print ende \n");
        expr = forall(var("x", t_userId),
                exists(var("p", t_UserPair),
                        eq(p, pair(x,x))));
        Object res = evaluatorImproved.eval(expr, structure);
       /* for (Object o : structure.values(t_UserPair)) {
            System.out.println(o.toString());
        }
        for (Object y : structure.values(t_userId)) {
            System.out.println(pair(x,x));
        }*/
        expr = ((QuantifierExpr)expr).getBody();
        expr = ((QuantifierExpr)expr).getBody();
        expr = ((App)expr).getArgs().get(1);
        System.out.println(((App) expr));
        System.out.println(((App) expr).getFunc());
        System.out.println(((App) expr).getArgs().get(1).getClass());
        assertEquals(true, res);

    }
    @Test
    public void test3Simple() {
        Set<Integer> visibleCalls = new HashSet<>();
        visibleCalls.add(1);
        visibleCalls.add(2);
        visibleCalls.add(3);
        HashMap<Integer, DatatypeValue> callOps = new LinkedHashMap<>();
        callOps.put(1, dataTypeValue("mapWrite", "User1", dataTypeValue("F_name"), "String1"));
        callOps.put(2, dataTypeValue("mapWrite", "User2", dataTypeValue("F_mail"), "String2"));
        callOps.put(3, dataTypeValue("mapDelete", "User2"));
        Set<DatatypeValue> happensBefore = new HashSet<>();
        happensBefore.add(pairValue(2,3));
        happensBefore.add(pairValue(3,1));
        /*        pairValue(2, 3),
                pairValue(3, 1)
        );*/
        String user = "User1";
        Structure structure = buildStructure(visibleCalls, callOps, happensBefore, user);

        Expr expr = mapExistsQuery();
        System.out.println(expr);
        Object res = evaluatorSimple.eval(expr, structure);
        assertEquals(true, res);

    }

    @Test
    public void test3() {
        Set<Integer> visibleCalls = new HashSet<>();
        visibleCalls.add(1);
        visibleCalls.add(2);
        visibleCalls.add(3);
        HashMap<Integer, DatatypeValue> callOps = new LinkedHashMap<>();
        callOps.put(1, dataTypeValue("mapWrite", "User1", dataTypeValue("F_name"), "String1"));
        callOps.put(2, dataTypeValue("mapWrite", "User2", dataTypeValue("F_mail"), "String2"));
        callOps.put(3, dataTypeValue("mapDelete", "User2"));
        Set<DatatypeValue> happensBefore = new HashSet<>();
        happensBefore.add(pairValue(2,3));
        happensBefore.add(pairValue(3,1));
        /*        pairValue(2, 3),
                pairValue(3, 1)
        );*/
        String user = "User1";
        Structure structure = buildStructure(visibleCalls, callOps, happensBefore, user);

        Expr expr = mapExistsQuery();
        System.out.println(expr);
        Object res = evaluatorImproved.eval(expr, structure);
        assertEquals(true, res);

    }


    /**
     * Delete-wins semantics for map:
     * An entry exists in the map, if there is a write operation such that all delete-operations
     * were executed before the write
     */
    private Expr mapExistsQuery() {
        Expr c1 = varuse("c1");
        Expr c2 = varuse("c2");
        Expr f = varuse("f");
        Expr v = varuse("v");
        // (∃c1: callId. ∃f: userRecordField. ∃v: string.
        //     c1 ∈ visibleCalls)
        //   ∧ op[c1] = mapWrite(user,f,v)
        //   ∧ (∀c2: callId. (c2 ∈ visibleCalls ∧ op[c2] = mapDelete(user)) ⟶ (pair(c2,c1) ∈ happensBefore)))

        return exists(var("c1", t_callId),
                exists(var("f", t_userRecordField),
                        exists(var("v", t_String),
                                and(
                                        contains(c1, c_visibleCalls),
                                        eq(get(c_op, c1), construct("mapWrite", c_user, f, v)),
                                        forall(var("c2", t_callId),
                                                implies(
                                                        and(
                                                                contains(c2, c_visibleCalls),
                                                                eq(get(c_op, c2), construct("mapDelete", c_user))
                                                        ),
                                                        contains(pair(c1, c2), c_happensBefore)
                                                )
                                        )
                                )
                        )
                )

        );

    }


    private Structure buildStructure(Set<Integer> visibleCalls, Map<Integer, DatatypeValue> callOps, Set<DatatypeValue> happensBefore, String user) {
        return new Structure() {

            private List<Object> strings = IntStream.range(1, 550).<Object>mapToObj(x -> "String" + x).collect(Collectors.toList());
            private List<Object> callIds = IntStream.range(1, 550).<Object>mapToObj(x -> x).collect(Collectors.toList());
            private List<Object> users = IntStream.range(1, 10).<Object>mapToObj(x -> "User" + x).collect(Collectors.toList());

            @Override
            public List<Object> valuesForCustomType(CustomType typ) {
                if (typ.equals(t_String)) {
                    return strings;
                } else if (typ.equals(t_callId)) {
                    return callIds;
                } else if (typ.equals(t_userId)) {
                    return users;
                } else {
                    throw new RuntimeException("unknown type: " + typ);
                }
            }

            @Override
            public Object interpretConstant(String f, Object[] args) {
                switch (f) {
                    case "visibleCalls":
                        return visibleCalls;
                    case "op":
                        return callOps;
                    case "happensBefore":
                        return happensBefore;
                    case "user":
                        return user;
                }
                throw new RuntimeException("TODO implement " + f);
            }

        };
    }

}
