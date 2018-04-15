package logiceval;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Created by Oliver on 21.11.2017.
 */
// TODO : eventuell aufteilen in mehrere Dateien und ein Package, printer implementieren, javaDocs
public class AbstractSyntax {

}
class ExprWrapper {
    Expr expr;

    public void setExpr(Expr expr) {
        this.expr = expr;
    }

    ExprWrapper(Expr expr) {
        this.expr = expr;
    }

    public Expr getExpr() {
        return expr;
    }
}
abstract class Expr {

    public String toString(){
        return PrintExpr.printExpr(this);
    }
    public boolean equals(Object o) {return this.toString().equals(o.toString());}
    public abstract void acceptEval(final ExprVisitor visitor);
    public abstract boolean acceptEquality(final ExprBooleanVisitor visitor);
}

class Variable {
    private String name;

    public void setTyp(Type typ) {
        this.typ = typ;
    }

    private Type typ;
    public Variable(String name, Type typ){
        this.name = name;
        this.typ = typ;
    }
    public String getName() {
        return name;
    }
    public Type getType() {
        return typ;
    }
    public boolean equals(Object o){return this.toString().equals(o.toString());}
    public String toString() { return name;}
}

class App extends Expr {
    private Func func;
    private List<Expr> args;
    public App(Func func, List<Expr> args) {
        this.func = func;
        this.args = args;
    }
    public Func getFunc() {
        return func;
    }
    public List<Expr> getArgs() {
        List<Expr> a = new ArrayList<>();
        a.addAll(args);
        return a;
    }
    public void setFunc(Func func) {
        this.func = func;
    }
    public void setArgs(List<Expr> args) {
        this.args = args;
    }
    public void acceptEval(final ExprVisitor visitor){
        visitor.visit(this);
    }
    public boolean acceptEquality(final ExprBooleanVisitor visitor){
        return visitor.visit(this);
    }
}

class QuantifierExpr extends Expr {
    private Quantifier quantifier;
    private Variable variable;
    private Expr body;
    public QuantifierExpr(Quantifier quantifier, Variable variable, Expr body) {
        this.quantifier = quantifier;
        this.variable = variable;
        this.body = body;
    }
    public Quantifier getQuantifier() {
        return quantifier;
    }
    public Variable getVariable() {
        return variable;
    }
    public Expr getBody() {
        return body;
    }
    public void acceptEval(final ExprVisitor visitor){
         visitor.visit(this);
    }

    public void setQuantifier(Quantifier quantifier) {
        this.quantifier = quantifier;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public void setBody(Expr body) {
        this.body = body;
    }
    public boolean acceptEquality(final ExprBooleanVisitor visitor){
        return visitor.visit(this);
    }
}

class Undef extends Expr{
    public void acceptEval(final ExprVisitor visitor){
        visitor.visit(this);
    }
    public boolean acceptEquality(final ExprBooleanVisitor visitor){
        return visitor.visit(this);
    }
}

class VarUse extends Expr {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public VarUse(String name) {
        this.name = name;

    }
    public String getName() {
        return name;
    }
    public void acceptEval(final ExprVisitor visitor){
        visitor.visit(this);
    }
    public boolean acceptEquality(final ExprBooleanVisitor visitor){
        return visitor.visit(this);
    }
}

class ConstantValue extends Expr {
    private Object value;
    public ConstantValue(Object value) {
        this.value = value;
    }
    public Object getValue() {
        return value;
    }
    public void acceptEval(final ExprVisitor visitor){
        visitor.visit(this);
    }
    public boolean acceptEquality(final ExprBooleanVisitor visitor){
        return visitor.visit(this);
    }
}

abstract class Quantifier {}

class Exists extends Quantifier {public String toString() {return "Exists";}}

class Forall extends Quantifier {public String toString() {return "Forall";}}

abstract class Func {
    public abstract void accept(final FuncVisitor visitor);
}

class Equals extends Func {
    public void accept(final FuncVisitor visitor) {
        visitor.visit(this);
    }
}

class And extends Func {
    public void accept(final FuncVisitor visitor) {
        visitor.visit(this);
    }
}

class Or extends Func {
    public void accept(final FuncVisitor visitor) {
        visitor.visit(this);
    }
}

class Implies extends Func {
    public void accept(final FuncVisitor visitor) {
        visitor.visit(this);
    }
}

class Not extends Func {
    public void accept(final FuncVisitor visitor) {
        visitor.visit(this);
    }
}

class Contains extends Func {
    public void accept(final FuncVisitor visitor) {
        visitor.visit(this);
    }
}

class Get extends Func {
    public void accept(final FuncVisitor visitor) {
        visitor.visit(this);
    }
}

class CFunc extends Func {
    private String name;
    public CFunc(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public void accept(final FuncVisitor visitor) {
        visitor.visit(this);
    }
}

class Construct extends Func {
    private String datatypeName;
    public Construct(String datatypeName) {
        this.datatypeName = datatypeName;
    }
    public String getDatatypeName() {
        return datatypeName;
    }
    public void accept(final FuncVisitor visitor) {
        visitor.visit(this);
    }
}

abstract class Type {}

class SetType extends Type {
    private Type elementType;
    public SetType(Type elementType) {
        this.elementType = elementType;
    }
    public Type getElementType() {
        return elementType;
    }
}

class SetTypeIterable extends Type {
    private Set<?> objectSet;
    private String name;
    public SetTypeIterable(Set<?> objectSet,String name) {
        this.objectSet = objectSet;
        this.name = name;
    }
    public Set<?> getObjectSet() {
        return objectSet;
    }
    public String getName(){
        return this.name;
    }
}


class MapType extends Type {
    private Type keyType;
    private Type valueType;
    public MapType(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }
    public Type getKeyType() {
        return keyType;
    }
    public Type getValueType() {
        return valueType;
    }
}

class DataTypeConstructor {
    private String name;
    private List<Type> fields;
    public DataTypeConstructor(String name, List<Type> fields) {
        this.name = name;
        this.fields = fields;
    }
    public String getName() {
        return name;
    }
    public List<Type> getFields() {
        return fields;
    }
}
class DataType extends Type {
    private String name;
    private Iterable<DataTypeConstructor> constructors;
    public DataType(String name, Iterable<DataTypeConstructor> constructors) {
        this.name = name;
        this.constructors = constructors;
    }
    public String getName() {
        return name;
    }
    public Iterable<DataTypeConstructor> getConstructors() {
        return constructors;
    }
}

class CustomType extends Type {
    private String name;
    public CustomType(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
class FunctionType {
    private List<Type> argTypes;
    private Type resultType;
    public FunctionType(List<Type> argTypes, Type resultType) {
        this.argTypes = argTypes;
        this.resultType = resultType;
    }
    public List<Type> getArgTypes() {
        return argTypes;
    }
    public Type getResultType() {
        return resultType;
    }
}

abstract class Value {}

class SimpleValue extends Value {
    private Object value;
    public SimpleValue(Object value) {
        this.value = value;
    }
    public Object getValue() {
        return value;
    }
}
class DatatypeValue extends Value {
    private String name;
    private List<?> values;
    public DatatypeValue(String name, List<?> values) {
        this.name = name;
        this.values = values;
    }
    public String getName() {
        return name;
    }
    public List<?> getValues() {
        return values;
    }
    public String toString() {
        // TODO name gewünscht?
        String ausgabe = name +", ";
        //String ausgabe = "";
        Iterator<?> iterator = values.iterator();
        while(iterator.hasNext()){
            ausgabe += iterator.next().toString() +", ";
        }
        //TODO was machen wenn kein iterable übergeben wurde
        if (ausgabe.length() <= 1) {
            return name;
        }
        return ausgabe.substring(0,ausgabe.length()-2);
    }
    @Override
    public boolean equals (Object o) {
        return toString().equals(o.toString());
    }
}

class UndefinedValue extends Value {}