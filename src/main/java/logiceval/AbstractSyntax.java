package logiceval;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by Oliver on 21.11.2017.
 */
// TODO : eventuell aufteilen in mehrere Dateien und ein Package, printer implementieren, javaDocs
public class AbstractSyntax {

}
abstract class Expr {
    public String toString(){
        return ""; // TODO : implement Printer
    }
}

class Variable {
    private String name;
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
        List<Expr> a = new ArrayList<Expr>();
        a.addAll(args);
        return a;
    }
}

class QuantifierExpr extends Expr{
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

}

class Undef extends Expr{}

class VarUse extends Expr {
    private String name;
    public VarUse(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
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
}

abstract class Quantifier {}

class Exists extends Quantifier {}

class Forall extends Quantifier {}

abstract class Func {}

class Equals extends Func {}

class And extends Func {}

class Or extends Func {}

class Implies extends Func {}

class Not extends Func {}

class Contains extends Func {}

class Get extends Func {}

class CFunc extends Func {
    private String name;
    public CFunc(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
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
    private List<DataTypeConstructor> constructors;
    public DataType(String name, List<DataTypeConstructor> constructors) {
        this.name = name;
        this.constructors = constructors;
    }
    public String getName() {
        return name;
    }
    public List<DataTypeConstructor> getConstructors() {
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
    private List<Object> values;
    public DatatypeValue(String name, List<Object> values) {
        this.name = name;
        this.values = values;
    }
    public String getName() {
        return name;
    }
    public List<Object> getValues() {
        return values;
    }
    public String toString() {
        String ausgabe = "";
        Iterator<Object> iterator = values.iterator();
        while(iterator.hasNext()){
            ausgabe += iterator.next().toString() +", ";
        }
        return ausgabe.substring(0,ausgabe.length()-2);
    }
}

class UndefinedValue extends Value {}