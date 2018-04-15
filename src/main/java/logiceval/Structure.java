package logiceval;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Oliver on 23.11.2017.
 */

public abstract class Structure {
    public abstract List<Object> valuesForCustomType(CustomType type);

    public abstract Object interpretConstant(String constantname, Object[] args);

    public  List<Object> values (Type typ) {
        ArrayList<Object> arrayList = new ArrayList<>();
        if (typ instanceof SetType) {
            throw new RuntimeException("Set-type is not enumerable");
        }
        else if (typ instanceof MapType) {
            throw new RuntimeException("Map-type is not enumerable");
        }
        else if (typ instanceof DataType) {
            for (DataTypeConstructor d: ((DataType) typ).getConstructors()) {

                for (List<Object> objects : valuesList(d.getFields())) {
                    arrayList.add(new DatatypeValue(d.getName(), objects));
                }
            }

            return arrayList;
        }
        else if (typ instanceof CustomType) {
            return valuesForCustomType((CustomType)typ);
        }
        else if (typ instanceof SetTypeIterable) {
            return new ArrayList<>(((SetTypeIterable) typ).getObjectSet());
        }
        else {
            throw new RuntimeException("Fall vergessen");
        }
    }

    private Iterable<List<Object>> valuesList(List<Type> fields) {
        ArrayList<List<Object>> arrayList = new ArrayList<>();
        Iterable<List<Object>> kr;
        List<Object> firstValues;
        if (fields.isEmpty()) {
            arrayList.add(new ArrayList<>());
            return arrayList;
        }
        if (fields.size() == 1) {
            Type first = fields.get(0);
            List<Object> firstList = (values(first));
            for (Object objects : firstList) {
                List<Object> ark = new ArrayList<>();
                ark.add(objects);
                arrayList.add(ark);
            }
            return arrayList;
        }
        else {
            Type first = fields.get(0);
            firstValues = values(first);
            // remove funktioniert nicht
            kr = valuesList(fields.subList(1,fields.size()));
        }
        for (Object o : firstValues) {
            for (List<Object> objectlists : kr) {
                List<Object> ark = new ArrayList<>(objectlists);
                ark.add(0,o);
                // TODO name gewünscht?
                //ark.add(0,name);
                arrayList.add(ark);
            }
        }
        return arrayList;
    }

}
