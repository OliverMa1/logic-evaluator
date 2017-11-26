package logiceval;

/**
 * Created by Oliver on 23.11.2017.
 */
//TODO
public interface Signatur {

    Type lookupType(String name);

    Type lookupConstant(String name);
}
