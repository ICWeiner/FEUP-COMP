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

    public static List<Type> parseParameters(String params) {
        if (params.equals("")) return new ArrayList<>();

        String[] typesString = params.split(",");

        List<Type> types = new ArrayList<>();

        for (String s : typesString) {
            String[] aux = s.split(" ");
            types.add(new Type(aux[0], aux.length == 2));
        }

        return types;
    }

    public static boolean matchParameters(List<Type> types1, List<Type> types2) {
        for (int i = 0; i < types1.size(); i++) {
            if (!types1.get(i).equals(types2.get(i))) {
                return false;
            }
        }
        return true;
    }

    public List<String> parametersToOllir() {
        List<String> ollir = new ArrayList<>();

        for (Map.Entry<Symbol, String> parameter : this.parameters) {
            ollir.add(OllirTemplates.variable(parameter.getKey()));
        }

        return ollir;
    }

    public List<Symbol> getLocalVariables() {
        return new ArrayList<>(this.localVars.keySet());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("JmmMethod").append("\n");

        builder.append("Name: ").append(methodName).append(" | Return: ").append(returnType.print()).append("\n");

        builder.append("Parameters").append("\n");
        for (Map.Entry<Symbol, String> param : this.parameters)
            builder.append("\t").append("Type: ").append(param.getKey().getType().print()).append(" Name: ").append(param.getKey().getName()).append("\n");

        builder.append("Local Variables").append("\n");
        for (Map.Entry<Symbol, Boolean> localVariable : this.localVars.entrySet()) {
            builder.append("\t").append("Type: ").append(localVariable.getKey().getType().print()).append(" Name: ").append(localVariable.getKey().getName()).append(" Initialized: ").append(localVariable.getValue()).append("\n");
        }

        return builder.toString();
    }

}
