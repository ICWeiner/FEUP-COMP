package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OllirVisitor extends AJmmVisitor<List<Object>, List<Object>> {
    private final SymbolTable table;
    private JmmMethod currentMethod;
    private final List<Report> reports;
    private String scope;
    private final Set<JmmNode> visited = new HashSet<>();

    private int temp_sequence = 1;

    public OllirVisitor(SymbolTable table,List<Report> reports){
        this.table = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        this.addVisit("Program",this::dealWithProgram);

        this.addVisit("ClassDeclaration",this::dealWithClass);
        this.addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        this.addVisit("ReturnDeclaration", this::dealWithReturn);

        this.addVisit("ExprStmt",this::dealWithExpression);

        this.addVisit("VarDeclaration", this::dealWithVarDeclaration);
        this.addVisit("Identifier", this::dealWithVariable);
        this.addVisit("Assignment", this::dealWithAssignment);
        this.addVisit("Integer", this::dealWithPrimitive);
        this.addVisit("Boolean", this::dealWithPrimitive);

        this.addVisit("BinaryOp", this::dealWithBinaryOperation);

        //this.addVisit("Identifier",this::dealWithIdentifier);



        setDefaultVisit(this::defaultVisit);
    }

    private List<Object> defaultVisit(JmmNode node, List<Object> data) {
        StringBuilder sb = new StringBuilder(node.getKind());
        sb.append(" DEFAULT_VISIT 1");
        return Collections.singletonList(sb.toString());
    }

    private List<Object> dealWithProgram(JmmNode node,List<Object> data){
        StringBuilder ollir = new StringBuilder();
        for (JmmNode child : node.getChildren()){
            if (child.getKind().equals("ImportDeclaration") ) continue;
            String ollirChild = (String) visit(child, Collections.singletonList("PROGRAM")).get(0);
            ollir.append(ollirChild);
        }
        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithClass(JmmNode node,List<Object> data){
        scope = "CLASS";


        List<String> fields = new ArrayList<>();
        List<String> classBody = new ArrayList<>();

        StringBuilder ollir = new StringBuilder();

        for (String importStmt : this.table.getImports()){
            ollir.append(String.format("import %s;\n",importStmt));
        }
        ollir.append("\n");

        for(JmmNode child : node.getChildren()){
            String ollirChild = (String) visit(child, Collections.singletonList("CLASS")).get(0);
            System.out.println(ollirChild);
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT")) {
                if (child.getKind().equals("VarDeclaration")) {
                    fields.add(ollirChild);
                } else {
                    classBody.add(ollirChild);
                }
            }
        }

        ollir.append(OllirTemplates.classTemplate(table.getClassName(), table.getSuper()));

        ollir.append(String.join("\n", fields)).append("\n\n");
        ollir.append(OllirTemplates.constructor(table.getClassName())).append("\n\n");
        ollir.append(String.join("\n\n", classBody));

        ollir.append(OllirTemplates.closeBrackets());

        return Collections.singletonList(ollir.toString());
    }


    private List<Object> dealWithMethodDeclaration(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 2");
        visited.add(node);

        scope = "METHOD";

        try {
            if (node.getKind().equals("MainMethod")){
                currentMethod = table.getMethod("main");
            }else{
                currentMethod = table.getMethod(node.getJmmChild(0).get("name"));//method attributes stored in first child
            }


        } catch (Exception e) {
            currentMethod = null;
            e.printStackTrace();
        }

        StringBuilder builder;

        if (node.getKind() == "MainMethod")
            builder = new StringBuilder(OllirTemplates.method(
                    "main",
                    currentMethod.parametersToOllir(),
                    OllirTemplates.type(currentMethod.getReturnType()),
                    true));
        else
            builder = new StringBuilder(OllirTemplates.method(
                    currentMethod.getName(),
                    currentMethod.parametersToOllir(),
                    OllirTemplates.type(currentMethod.getReturnType())));


        List<String> body = new ArrayList<>();

        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Type")) continue;
            String ollirChild = (String) visit(child, Collections.singletonList("METHOD")).get(0);
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
            body.add(ollirChild);
        }

        builder.append(String.join("\n", body));

        builder.append(OllirTemplates.closeBrackets());

        return Collections.singletonList(builder.toString());
    }

    private List<Object> dealWithVarDeclaration(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 3");
        visited.add(node);

        if ("CLASS".equals(data.get(0))) {
            Map.Entry<Symbol, Boolean> variable = table.getField(node.getJmmChild(0).get("name"));
            return Arrays.asList(OllirTemplates.field(variable.getKey()));
        }/*else if ("METHOD".equals(data.get(0))) {
            JmmNode child = node.getJmmChild(0);
            Symbol s = new Symbol(new Type(child.get("typeName"), (Boolean) child.getObject("isArray")), child.get("name"));
            return Arrays.asList(OllirTemplates.localfield(s));
        }*/

        return Arrays.asList("");//TODO: probably change
    }

    private List<Object> dealWithAssignment(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 5");
        visited.add(node);

        Map.Entry<Symbol, Boolean> variable;
        boolean classField = false;

        if ((variable = currentMethod.getField(node.get("name"))) == null) {
            variable = table.getField(node.get("name"));
            classField = true;
        }
        String name = !classField ? currentMethod.isParameter(variable.getKey()) : null;

        String ollirVariable;
        String ollirType;

        StringBuilder ollir = new StringBuilder();

        ollirVariable = OllirTemplates.variable(variable.getKey(), name);
        ollirType = OllirTemplates.type(variable.getKey().getType());

        List<Object> visitResult;

        // ARRAY ACCESS
        if (node.getChildren().size() > 1) {
            String target = (String) visit(node.getChildren().get(0)).get(0);
            String[] parts = target.split("\n");
            if (parts.length > 1) {
                for (int i = 0; i < parts.length - 1; i++) {
                    ollir.append(parts[i]).append("\n");
                }
            }

            visitResult = visit(node.getChildren().get(1), Arrays.asList(classField ? "FIELD" : "ASSIGNMENT", variable.getKey(), "ARRAY_ACCESS"));

            String result = (String) visitResult.get(0);
            String[] parts2 = result.split("\n");

            if (parts2.length > 1) {
                for (int i = 0; i < parts2.length - 1; i++) {
                    ollir.append(parts2[i]).append("\n");
                }

                if (!classField) {
                    String temp = "temporary" + temp_sequence++ + ".i32";
                    ollir.append(String.format("%s :=.i32 %s;\n", temp, parts2[parts2.length - 1]));

                    ollir.append(String.format("%s :=%s %s;\n",
                            OllirTemplates.arrayaccess(variable.getKey(), name, parts[parts.length - 1]),
                            OllirTemplates.type(new Type(variable.getKey().getType().getName(), false)),
                            temp));
                } else {
                    String temp = "temporary" + temp_sequence++;

                    ollir.append(String.format("%s :=%s %s;\n", temp + ollirType, ollirType, OllirTemplates.getfield(variable.getKey())));

                    ollir.append(String.format("%s :=%s %s;\n",
                            OllirTemplates.arrayaccess(new Symbol(new Type("int", true), temp), null, parts[parts.length - 1]),
                            OllirTemplates.type(new Type(variable.getKey().getType().getName(), false)),
                            parts2[parts2.length - 1]));
                }
            } else {
                if (!classField) {
                    String temp = "temporary" + temp_sequence++ + ".i32";
                    ollir.append(String.format("%s :=.i32 %s;\n", temp, result));

                    ollir.append(String.format("%s :=%s %s;\n",
                            OllirTemplates.arrayaccess(variable.getKey(), name, parts[parts.length - 1]),
                            OllirTemplates.type(new Type(variable.getKey().getType().getName(), false)),
                            temp));
                } else {
                    String temp = "temporary" + temp_sequence++;

                    ollir.append(String.format("%s :=%s %s;\n", temp + ollirType, ollirType, OllirTemplates.getfield(variable.getKey())));

                    ollir.append(String.format("%s :=%s %s;\n",
                            OllirTemplates.arrayaccess(new Symbol(new Type("int", true), temp), null, parts[parts.length - 1]),
                            OllirTemplates.type(new Type(variable.getKey().getType().getName(), false)),
                            result));
                }
            }
        } else {
            visitResult = visit(node.getChildren().get(0), Arrays.asList(classField ? "FIELD" : "ASSIGNMENT", variable.getKey(), "SIMPLE"));

            String result = (String) visitResult.get(0);
            String[] parts = result.split("\n");

            if (parts.length > 1) {
                for (int i = 0; i < parts.length - 1; i++) {
                    ollir.append(parts[i]).append("\n");
                }
                if (!classField) {
                    ollir.append(String.format("%s :=%s %s;", ollirVariable, ollirType, parts[parts.length - 1]));
                } else {
                    if (visitResult.size() > 1 && (visitResult.get(1).equals("ARRAY_INIT") || visitResult.get(1).equals("OBJECT_INIT"))) {
                        String temp = "temporary" + temp_sequence++ + ollirType;
                        ollir.append(String.format("%s :=%s %s;\n", temp, ollirType, parts[parts.length - 1]));
                        ollir.append(OllirTemplates.putfield(ollirVariable, temp));
                    } else {
                        ollir.append(OllirTemplates.putfield(ollirVariable, parts[parts.length - 1]));
                    }
                    ollir.append(";");
                }
            } else {
                if (!classField) {
                    ollir.append(String.format("%s :=%s %s;", ollirVariable, ollirType, result));
                } else {
                    if (visitResult.size() > 1 && (visitResult.get(1).equals("ARRAY_INIT") || visitResult.get(1).equals("OBJECT_INIT"))) {
                        String temp = "temporary" + temp_sequence++ + ollirType;
                        ollir.append(String.format("%s :=%s %s;\n", temp, ollirType, result));
                        ollir.append(OllirTemplates.putfield(ollirVariable, temp));
                    } else {
                        ollir.append(OllirTemplates.putfield(ollirVariable, result));
                    }
                    ollir.append(";");
                }
            }
        }


        if (visitResult.size() > 1 && visitResult.get(1).equals("OBJECT_INIT")) {
            ollir.append("\n").append(OllirTemplates.objectinstance(variable.getKey()));
        }

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithPrimitive(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 6");
        visited.add(node);

        String value;
        String type;

        if (node.getKind().equals("Integer")){
            value = node.get("value") + ".i32";
            type = ".i32";
        }else if(node.getKind().equals("BooleanLiteral")){
            value = (node.get("value").equals("true") ? "1" : "0") + ".bool";
            type = ".bool";
        }else{
            value = "";
            type = "";
        }


        if (data.get(0).equals("RETURN")) {
            String temp = "temporary" + temp_sequence++ + type;
            value = String.format("%s :=%s %s;\n%s", temp, type, value, temp);
        } else if (data.get(0).equals("CONDITION") && type.equals(".bool")) {
            value = String.format("%s ==.bool 1.bool\n", value);
        }

        return Collections.singletonList(value);
    }


    private List<Object> dealWithBinaryOperation(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 7");
        visited.add(node);

        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        String leftReturn = (String) visit(left, Collections.singletonList("BINARY")).get(0);
        String rightReturn = (String) visit(right, Collections.singletonList("BINARY")).get(0);

        String[] leftStmts = leftReturn.split("\n");
        String[] rightStmts = rightReturn.split("\n");

        StringBuilder ollir = new StringBuilder();

        String leftSide;
        String rightSide;

        leftSide = binaryOperations(leftStmts, ollir, new Type("int", false));
        rightSide = binaryOperations(rightStmts, ollir, new Type("int", false));

        if (data == null) {
            return Arrays.asList("DEFAULT_VISIT 8");
        }
        if (data.get(0).equals("RETURN") || data.get(0).equals("FIELD")) {
            Symbol variable = new Symbol(new Type("int", false), "temporary" + temp_sequence++);
            ollir.append(String.format("%s :=.i32 %s %s.i32 %s;\n", OllirTemplates.variable(variable), leftSide, node.get("op"), rightSide));
            ollir.append(OllirTemplates.variable(variable));
        } else {
            ollir.append(String.format("%s %s.i32 %s", leftSide, node.get("op"), rightSide));
        }

        return Collections.singletonList(ollir.toString());
    }


    private List<Object> dealWithVariable(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        Map.Entry<Symbol, Boolean> field = null;

        boolean classField = false;
        if (scope.equals("CLASS")) {
            classField = true;
            field = table.getField(node.get("name"));
        } else if (scope.equals("METHOD") && currentMethod != null) {
            field = currentMethod.getField(node.get("value"));
            if (field == null) {
                classField = true;
                field = table.getField(node.get("name"));
            }
        }

        StringBuilder superiorOllir = null;
        if (data.get(0).equals("ACCESS")) {
            superiorOllir = (StringBuilder) data.get(1);
        }

        if (field != null) {
            String name = currentMethod.isParameter(field.getKey());
            if (classField && !scope.equals("CLASS")) {
                StringBuilder ollir = new StringBuilder();
                Symbol variable = new Symbol(field.getKey().getType(), "temporary" + temp_sequence++);
                if (data.get(0).equals("CONDITION")) {
                    ollir.append(String.format("%s :=%s %s;\n",
                            OllirTemplates.variable(variable),
                            OllirTemplates.type(variable.getType()),
                            OllirTemplates.getfield(field.getKey())));
                    ollir.append(String.format("%s ==.bool 1.bool", OllirTemplates.variable(variable)));

                    return Arrays.asList(ollir.toString(), variable, name);
                } else {
                    Objects.requireNonNullElse(superiorOllir, ollir).append(String.format("%s :=%s %s;\n",
                            OllirTemplates.variable(variable),
                            OllirTemplates.type(variable.getType()),
                            OllirTemplates.getfield(field.getKey())));

                    ollir.append(OllirTemplates.variable(variable));
                    return Arrays.asList(ollir.toString(), variable, name);
                }
            } else {
                if (data.get(0).equals("CONDITION")) {
                    return Arrays.asList(String.format("%s ==.bool 1.bool", OllirTemplates.variable(field.getKey(), name)), field.getKey(), name);
                }

                return Arrays.asList(OllirTemplates.variable(field.getKey(), name), field.getKey(), name);
            }
        }
        return Arrays.asList("ACCESS", node.get("name"));
    }

    private List<Object> dealWithReturn(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        List<Object> visit = visit(node.getChildren().get(0), Arrays.asList("RETURN"));

        String result = (String) visit.get(0);
        String[] parts = result.split("\n");
        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) {
                ollir.append(parts[i]).append("\n");
            }
            ollir.append(OllirTemplates.ret(currentMethod.getReturnType(), parts[parts.length - 1]));
        } else {
            ollir.append(OllirTemplates.ret(currentMethod.getReturnType(), result));
        }

        return Collections.singletonList(ollir.toString());
    }

    private  List<Object> dealWithExpression(JmmNode node, List<Object> data){
        StringBuilder ollir = new StringBuilder();
        for (JmmNode child : node.getChildren()){
            //if (child.getKind().equals("ImportDeclaration") ) continue;
            String ollirChild = (String) visit(child, Collections.singletonList("EXPRESSION")).get(0);
            ollir.append(ollirChild);
        }
        return Collections.singletonList(ollir.toString());
    }

    

    private String binaryOperations(String[] statements, StringBuilder ollir, Type type) {
        String finalStmt;
        if (statements.length > 1) {
            for (int i = 0; i < statements.length - 1; i++) {
                ollir.append(statements[i]).append("\n");
            }
            String last = statements[statements.length - 1];
            if (last.split("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)").length == 2) {
                Pattern p = Pattern.compile("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)");
                Matcher m = p.matcher(last);

                m.find();

                ollir.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence))), OllirTemplates.assignmentType(m.group(1)), last));
                finalStmt = OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence++)));
            } else {
                finalStmt = last;
            }
        } else {
            if (statements[0].split("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)").length == 2) {
                Pattern p = Pattern.compile("(/|\\+|-|\\*|&&|<|!|>=)(\\.i32|\\.bool)");
                Matcher m = p.matcher(statements[0]);
                m.find();

                ollir.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence))), OllirTemplates.assignmentType(m.group(1)), statements[0]));
                finalStmt = OllirTemplates.variable(new Symbol(type, String.format("temporary%d", temp_sequence++)));
            } else {
                finalStmt = statements[0];
            }
        }
        return finalStmt;
    }
}
