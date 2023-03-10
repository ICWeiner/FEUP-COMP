package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmmMethod {
    private String methodName;
    private Type returnType;
    private List<Symbol> parameters;
    private final Map<String,Symbol> localVars = new HashMap<>();

    public JmmMethod(String methodName, Type returnType, List<Symbol> parameters){
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = parameters;
        for(Symbol parameter : parameters)
            localVars.put(parameter.getName(),parameter);
    }
}
