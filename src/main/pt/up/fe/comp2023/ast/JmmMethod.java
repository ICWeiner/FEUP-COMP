package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JmmMethod {
    private String methodName;
    private Type returnType;
    private final List<Map.Entry<Symbol, String>> parameters = new ArrayList<>();
    private final Map<Symbol, Boolean> localVars = new HashMap<>();


    public JmmMethod(String methodName, Type returnType) {
        this.methodName = methodName;
        this.returnType = returnType;
    }

    public List<Type> getParameterTypes() {
        List<Type> params = new ArrayList<>();

        for (Map.Entry<Symbol, String> parameter : parameters) {
            params.add(parameter.getKey().getType());
        }
        return params;
    }

    public void addLocalVariable(Symbol variable) {
        localVars.put(variable, false);
    }

    public String getName() {
        return methodName;
    }

    public void setName(String name) {
        this.methodName = name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public void addParameter(Symbol param) {
        this.parameters.add(Map.entry(param, "param"));
    }

    public boolean fieldExists(String field) {
        for (Symbol localVariable : this.localVars.keySet()) {
            if (localVariable.getName().equals(field))
                return true;
        }
        return false;
    }

    public Map.Entry<Symbol, Boolean> getField(String name) {
        for (Map.Entry<Symbol, Boolean> field : this.localVars.entrySet()) {
            if (field.getKey().getName().equals(name))
                return field;
        }

        for (Map.Entry<Symbol, String> param : this.parameters) {
            if (param.getKey().getName().equals(name))
                return Map.entry(param.getKey(), true);
        }

        return null;
    }

    public boolean initializeField(Symbol symbol) {
        if (this.localVars.containsKey(symbol)) {
            this.localVars.put(symbol, true);
            return true;
        }
        return false;
    }

    public List<Symbol> getParameters() {
        List<Symbol> params = new ArrayList<>();
        for (Map.Entry<Symbol, String> param : this.parameters) {
            params.add(param.getKey());
        }
        return params;
    }

    public List<Symbol> getLocalVariables() {
        return new ArrayList<>(this.localVars.keySet());
    }

}
