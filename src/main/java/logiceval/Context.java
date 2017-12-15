package logiceval;

import java.util.Map;

/**
 * Created by Oliver on 14.12.2017.
 */
public class Context {

    private Structure structure;
    private Map<String,Object> localVars;
    public Context(Structure structure, Map<String,Object> localVars) {
        this.structure = structure;
        this.localVars = localVars;
    }

    public Structure getStructure() {
        return structure;
    }

    public Map<String, Object> getLocalVars() {
        return localVars;
    }
    public void setStructure(Structure structure) {
        this.structure = structure;
    }
}