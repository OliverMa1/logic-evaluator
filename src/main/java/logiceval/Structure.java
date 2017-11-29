package logiceval;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Oliver on 23.11.2017.
 */

public abstract class Structure {
    public abstract Iterable<Object> valuesForCustomType(CustomType type);

    public abstract Object interpretConstant(String constantname, Object[] args);

    public  Iterable<Object> values (Type typ) {
        ArrayList<Object> arrayList = new ArrayList<>();
        if (typ instanceof SetType) {
            throw new RuntimeException("Set-type is not enumerable");
        }
        else if (typ instanceof MapType) {
            throw new RuntimeException("Map-type is not enumerable");
        }
        else if (typ instanceof DataType) {
            for (DataTypeConstructor d: ((DataType) typ).getConstructors()) {

                for (Iterable<Object> objects : valuesList(d.getFields())) {
                    for (Object o : objects) {
                    }
                    arrayList.add(new DatatypeValue(d.getName(), objects));
                }
            }
            if (arrayList.isEmpty()) {
                //TODO was machen wenn fields leer ist
                for (DataTypeConstructor d: ((DataType) typ).getConstructors()) {
                    arrayList.add(d.getName());
                }
            }
            return arrayList;
        }
        else if (typ instanceof CustomType) {
            return valuesForCustomType((CustomType)typ);
        }
        else {
            throw new RuntimeException("Fall vergessen");
        }
    }

    private Iterable<Iterable<Object>> valuesList(Iterable<Type> fields) {
        Iterator<Type> iterator = fields.iterator();
        ArrayList<Iterable<Object>> arrayList = new ArrayList<>();
        while (iterator.hasNext()) {
            arrayList.add(values(iterator.next()));
        }
        return arrayList;
    }

}
