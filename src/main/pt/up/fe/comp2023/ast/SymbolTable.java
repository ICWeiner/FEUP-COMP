package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.ast.exceptions.NoSuchMethod;


import java.util.*;

public class SymbolTable implements pt.up.fe.comp.jmm.analysis.table.SymbolTable {
    private List<Report> reports; // do i need this??
    private String superClassName;
    private String className;
    private JmmMethod currentMethod;

    private final List<String> imports = new ArrayList<>();
    private final Map<Symbol, Boolean> fields = new HashMap<>();
    private final List<JmmMethod> methods = new ArrayList<>();

    public static Type getType(JmmNode node, String attribute,Boolean isArray) {//TODO: Maybe unnecessary
        Type type;
        if (node.get(attribute).equals("int"))
            type = new Type("int", isArray);
        else
            type = new Type(node.get(attribute), false);

        return type;
    }


    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getSuper() {
        return superClassName;
    }

    public void setSuper(String superClassName) {
        this.superClassName = superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return new ArrayList<>(this.fields.keySet());
    }

    public boolean fieldExists(String name) {
        for (Symbol field : this.fields.keySet()) {
            if (field.getName().equals(name))
                return true;
        }
        return false;
    }

    @Override
    public List<String> getMethods() {
        List<String> methods = new ArrayList<>();
        for (JmmMethod method : this.methods) {
            methods.add(method.getName());
        }

        return methods;
    }

    public JmmMethod getMethod(String name) throws NoSuchMethod {
        for (JmmMethod method : methods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }

        throw new NoSuchMethod(name);
    }

    public JmmMethod getMethod(String name, List<Type> params, Type returnType) throws NoSuchMethod {//To support overloading
        for (JmmMethod method : methods) {
            if (method.getName().equals(name) && returnType.equals(method.getReturnType()) && params.size() == method.getParameters().size()) {
                if (JmmMethod.matchParameters(params, method.getParameterTypes())) {
                    return method;
                }
            }
        }

        throw new NoSuchMethod(name);
    }

    @Override
    public Type getReturnType(String methodName) {
        List<Type> params = new ArrayList<>();
        String[] parts = methodName.split("::");
        methodName = parts[0];

        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                String[] parts2 = parts[i].split(":");
                params.add(new Type(parts2[0], parts2[1].equals("true")));
            }
        } else {
            for (JmmMethod method : methods) {
                if(method.getName().equals(methodName)) {
                    return method.getReturnType();
                }
            }
        }

        for (JmmMethod method : methods) {
            if(method.getName().equals(methodName)) {
                List<Symbol> currentparams = method.getParameters();
                boolean found = true;
                if (currentparams.size() != params.size()) continue;
                for (int i=0; i<params.size(); i++) {
                    if (!currentparams.get(i).getType().equals(params.get(i))) {
                        found = false;
                        break;
                    }
                }
                if (found) return method.getReturnType();
            }
        }
        return null;
    }

    @Override
    public List<Symbol> getParameters(String s) {
        for (JmmMethod method : this.methods){
            if (method.getName().equals(s)){
                return method.getParameters();
            }
        }
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        for(JmmMethod method : methods)
            if (s.equals(method.getName()))
                return method.getLocalVariables();
        return null;
    }

    public Type getVariableType(String s, String method) {
        List<Symbol> localVariables = getLocalVariables(method);
        for (Symbol localVariable : localVariables) {
            if (localVariable.getName().equals(s)) {
                return localVariable.getType();
            }
        }
        List<Symbol> parameters = getParameters(method);
        for (Symbol parameter : parameters) {
            if (parameter.getName().equals(s)) {
                return parameter.getType();
            }
        }
        List<String> imports = getImports();
        for (String i : imports) {
            if (s.equals(i)) {
                return new Type(i,false);
            }
        }

        List<Symbol> fields = getFields();
        for (Symbol field : fields) {
            if (field.getName().equals(s)) {
                return field.getType();
            }
        }

        return null;
    }

    public JmmMethod getCurrentMethod() {
        return currentMethod;
    }

    public void addMethod(String name, Type returnType) {
        currentMethod = new JmmMethod(name, returnType);
        methods.add(currentMethod);
    }

    public void addFieldToCurrentMethod(Symbol field){
        if (currentMethod == null) return;
        currentMethod.addLocalVariable(field);
    }

    public void addParameterToCurrentMethod(Symbol param){
        if (currentMethod == null) return;
        currentMethod.addParameter(param);
    }

    public void addImport(String importStatement) {
        imports.add(importStatement);
    }

    public void addField(Symbol field) {
        fields.put(field, false);
    }

    public Map.Entry<Symbol, Boolean> getField(String name) {
        for (Map.Entry<Symbol, Boolean> field : this.fields.entrySet()) {
            if (field.getKey().getName().equals(name))
                return field;
        }
        return null;
    }

    @Override
    public String print() {
        return pt.up.fe.comp.jmm.analysis.table.SymbolTable.super.print();
    }
    @Override
    public String toString() {
        return pt.up.fe.comp.jmm.analysis.table.SymbolTable.super.print();
    }

}
