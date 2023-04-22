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
        this.addVisit("This", this::dealWithVariable);
        this.addVisit("Assignment", this::dealWithAssignment);
        this.addVisit("Integer", this::dealWithType);
        this.addVisit("Boolean", this::dealWithType);
        this.addVisit("GeneralDeclaration", this::dealWithObjectInit);

        this.addVisit("BinaryOp", this::dealWithBinaryOperation);
        this.addVisit("MethodCall", this::dealWithMethodCall);//why doesnt merge work?????


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
            System.out.println(ollirChild); //TODO:this prints the code, find another way to do it?
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

        if (node.getKind().equals("MainMethod"))
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

            System.out.println("visitResult is: " + visitResult);
            System.out.println("Node is of kind: " + node.getKind());
            System.out.println("Targer node is of kind: " + node.getChildren().get(0).getKind());

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

    private List<Object> dealWithObjectInit(JmmNode node, List<Object> data){
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 6");
        visited.add(node);

        String toReturn = OllirTemplates.objectinit(node.get("name"));
        if (data.get(0).equals("METHOD")) {
            toReturn += ";";
        }
        return Arrays.asList(toReturn, "OBJECT_INIT", node.get("name"));

    }

    private List<Object> dealWithType(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 6");
        visited.add(node);

        String value;
        String type;

        if (node.getKind().equals("Integer")){
            value = node.get("value") + ".i32";
            type = ".i32";
        }else if(node.getKind().equals("Boolean")){
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

        if(node.getKind().equals("This")){
            return Arrays.asList("ACCESS", "this");
        }



        Map.Entry<Symbol, Boolean> field = null;


        boolean classField = false;
        if (scope.equals("CLASS")) {
            classField = true;
            field = table.getField(node.get("name"));
        } else if (scope.equals("METHOD") && currentMethod != null) {
            field = currentMethod.getField(node.get("value"));
            if (field == null) {
                classField = true;
                field = table.getField(node.get("value"));
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
                    System.out.println("RETURN ON 1");
                    return Arrays.asList(ollir.toString(), variable, name);
                } else {
                    Objects.requireNonNullElse(superiorOllir, ollir).append(String.format("%s :=%s %s;\n",
                            OllirTemplates.variable(variable),
                            OllirTemplates.type(variable.getType()),
                            OllirTemplates.getfield(field.getKey())));

                    ollir.append(OllirTemplates.variable(variable));
                    System.out.println("RETURN ON 2");
                    return Arrays.asList(ollir.toString(), variable, name);
                }
            } else {
                if (data.get(0).equals("CONDITION")) {
                    System.out.println("RETURN ON 3");
                    return Arrays.asList(String.format("%s ==.bool 1.bool", OllirTemplates.variable(field.getKey(), name)), field.getKey(), name);
                }
                System.out.println("RETURN ON 4");
                return Arrays.asList(OllirTemplates.variable(field.getKey(), name), field.getKey(), name);
            }
        }
        System.out.println("RETURN ON 5");
        return Arrays.asList("ACCESS", node.get("value"));
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

    private List<Object> dealWithThis(JmmNode node, List<Object> data){
        /*if (targetReturn.get(0).equals("ACCESS")) {
            // Static Imported Methods
            if (!targetReturn.get(1).equals("this")) {*/
        return null;
    }

    private  List<Object> dealWithExpression(JmmNode node, List<Object> data){
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();
        for (JmmNode child : node.getChildren()){
            String ollirChild = (String) visit(child, Collections.singletonList("EXPR_STMT")).get(0);
            ollir.append(ollirChild);
        }
        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithMethodCall(JmmNode node, List<Object> data) {//TODO: fix when son of binary op or "="
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        String methodClass;

        System.out.println("Visiting method call with name: " + node.get("value"));

        JmmNode targetNode = node.getChildren().get(0);
        JmmNode methodNode = node;

        StringBuilder ollir = new StringBuilder();


        List<Object> targetReturn = visit(targetNode, Arrays.asList("ACCESS", ollir));
        //List<Object> methodReturn = visit(methodNode, Arrays.asList("ACCESS", ollir));

        //###########################################################
        //StringBuilder ollir = (StringBuilder) data.get(1); TODO: ver qual era a necessidade disto - nenhuma aparentemente


        List<JmmNode> children = node.getChildren();
        children.remove(0);//remove first node as it isnt a parameter TODO:modify grammar?
        Map.Entry<List<Type>, String> params = getParametersList(children, ollir);

        String methodString = node.get("value");
        if (params.getKey().size() > 0) {
            for (Type param : params.getKey()) {
                methodString += "::" + param.getName() + ":" + (param.isArray() ? "true" : "false");
            }
        }
        Type returnType = table.getReturnType(methodString);
        System.out.println("methodString is: " + methodString);
        JmmMethod method;

        //ver se identifier é  ou classe do proprio ou objeto da classe propria
        try {
            method = table.getMethod(methodNode.get("value"), params.getKey(), returnType);
            methodClass = "class_method";
            System.out.println("methodClass is: " + methodClass);

            var identifierType = currentMethod.getField(targetNode.get("value")).getKey().getType().getName();
            System.out.println("identifierType is: " + identifierType);
            for( var importName : table.getImports() ){
                if (!targetNode.get("value").equals(importName) && !identifierType.equals(importName)){
                    System.out.println("importname is:" + importName);
                    System.out.println("targetNode is:" + targetNode.get("value"));
                    System.out.println("identifierType is:" + identifierType);
                    continue;
                }
                methodClass = "method";
            }



            System.out.println("methodClass is: " + methodClass);



        } catch (Exception e) {
            method = null;
            methodClass = "method";
        }
        //###########################################################


        Symbol assignment = (data.get(0).equals("ASSIGNMENT")) ? (Symbol) data.get(1) : null;

        System.out.println("assignment is: " + assignment );

        String ollirExpression = null;
        Type expectedType = (data.get(0).equals("BINARY") || (data.size() > 2 && data.get(2).equals("ARRAY_ACCESS"))) ? new Type("int", false) : null;


        if (targetReturn.get(0).equals("ACCESS")) {
            // Static Imported Methods
            if (!targetReturn.get(1).equals("this")) {
                System.out.println("TargetReturn on dealwithmethodcall is:" + targetReturn);
                String targetVariable = (String) targetReturn.get(1);
                if (assignment != null) {
                    if (data.get(2).equals("ARRAY_ACCESS")) { /*TODO: Fix since changes
                        ollirExpression = OllirTemplates.invokestatic(targetVariable, (String) methodReturn.get(1), new Type(assignment.getType().getName(), false), (String) params.getValue());
                        expectedType = new Type(assignment.getType().getName(), false);*/
                    } else {
                        ollirExpression = OllirTemplates.invokestatic(targetVariable,  methodNode.get("value"), assignment.getType(),  params.getValue());
                        expectedType = assignment.getType();
                    }
                } else {
                    expectedType = (expectedType == null) ? new Type("void", false) : expectedType;
                    ollirExpression = OllirTemplates.invokestatic(targetVariable,  node.get("value"), expectedType,  params.getValue());
                }
            } else {
                // imported method called on "this"
                if (methodClass.equals("method")) {

                    if (assignment != null) {
                        ollirExpression = OllirTemplates.invokespecial( node.get("value"), assignment.getType(),  params.getValue());
                        expectedType = assignment.getType();
                    } else {
                        expectedType = (expectedType == null) ? new Type("void", false) : expectedType;
                        ollirExpression = OllirTemplates.invokespecial( node.get("value"), expectedType,  params.getValue());
                    }
                } else {
                    // Declared method called on "this
                    ollirExpression = OllirTemplates.invokevirtual(method.getName(), method.getReturnType(),  params.getValue());
                    expectedType = method.getReturnType();
                }
            }
        } else if (methodNode.getKind().equals("ArrayAccess")) {
            // ARRAY ACCESS TODO:ADAPT SINCE MAJOR CHANGES
            /*Symbol array = (Symbol) targetReturn.get(1);
            String index = (String) methodReturn.get(0);

            String[] parts = index.split("\n");
            if (parts.length > 1) {
                for (int i = 0; i < parts.length - 1; i++) {
                    ollir.append(parts[i]).append("\n");
                }
            }

            ollirExpression = OllirTemplates.arrayaccess(array, (String) targetReturn.get(2), parts[parts.length - 1]);
            expectedType = new Type(array.getType().getName(), false);*/
        } else {
            if (targetReturn.get(1).equals("OBJECT_INIT")) {
                Type type = new Type((String) targetReturn.get(2), false);
                Symbol auxiliary = new Symbol(type, "temporary" + temp_sequence++);
                ollir.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(auxiliary), OllirTemplates.type(type), targetReturn.get(0)));
                ollir.append(OllirTemplates.objectinstance(auxiliary)).append("\n");

                if (methodClass.equals("method")) {
                    if (assignment != null) {
                        ollirExpression = OllirTemplates.invokespecial(
                                OllirTemplates.variable(auxiliary),
                                methodNode.get("Value"),
                                assignment.getType(),
                                params.getValue()
                        );
                        expectedType = assignment.getType();
                    } else {
                        expectedType = (expectedType == null) ? new Type("void", false) : expectedType;
                        ollirExpression = OllirTemplates.invokespecial(
                                OllirTemplates.variable(auxiliary),
                                methodNode.get("Value"),
                                expectedType,
                                params.getValue()
                        );
                    }

                } else {
                    // Declared method called on "this"
                    ollirExpression = OllirTemplates.invokevirtual(OllirTemplates.variable(auxiliary), method.getName(), method.getReturnType(), params.getValue());
                    expectedType = method.getReturnType();
                }
            } else {
                if (methodClass.equals("method")) {

                    if (assignment != null) {
                        ollirExpression = OllirTemplates.invokespecial(OllirTemplates.variable((Symbol) targetReturn.get(1)),  methodNode.get("value"), assignment.getType(), params.getValue());
                        expectedType = assignment.getType();
                    } else {//TODO ENTROU AQUI

                        expectedType = (expectedType == null) ? new Type("void", false) : expectedType;
                        ollirExpression = OllirTemplates.invokespecial(OllirTemplates.variable((Symbol) targetReturn.get(1)), params.getValue(), expectedType,  params.getValue());
                    }
                } else if (!methodClass.equals("length")) {//TODO MAS DEVIA TER ENTRADO AQUI
                    Symbol targetVariable = (Symbol) targetReturn.get(1);

                    System.out.println("ESTOU AQUI");
                    ollirExpression = OllirTemplates.invokevirtual(OllirTemplates.variable(targetVariable), method.getName(), method.getReturnType(), params.getValue());
                    expectedType = method.getReturnType();
                }
            }
        }

        if ((data.get(0).equals("CONDITION") || data.get(0).equals("BINARY") || data.get(0).equals("FIELD") || data.get(0).equals("PARAM") || data.get(0).equals("RETURN")) && expectedType != null && ollirExpression != null) {
            Symbol auxiliary = new Symbol(expectedType, "temporary" + temp_sequence++);
            ollir.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(auxiliary), OllirTemplates.type(expectedType), ollirExpression));
            if (data.get(0).equals("CONDITION")) {
                ollir.append(String.format("%s ==.bool 1.bool", OllirTemplates.variable(auxiliary)));
            } else if (data.get(0).equals("BINARY") || data.get(0).equals("FIELD") || data.get(0).equals("PARAM") || data.get(0).equals("RETURN")) {
                ollir.append(String.format("%s", OllirTemplates.variable(auxiliary)));
            }
        } else {
            ollir.append(ollirExpression);
        }


        if (data.get(0).equals("EXPR_STMT")||data.get(0).equals("METHOD") || data.get(0).equals("IF") || data.get(0).equals("ELSE") || data.get(0).equals("WHILE")) {
            ollir.append(";");
        }

        return Arrays.asList(ollir.toString(), expectedType);
    }

    private Map.Entry<List<Type>, String> getParametersList(List<JmmNode> children, StringBuilder ollir) {

        List<Type> params = new ArrayList<>();
        List<String> paramsOllir = new ArrayList<>();

        for (JmmNode child : children) {
            Type type;
            String var;
            String[] statements;
            String result;
            switch (child.getKind()) {
                case "Integer":
                    type = new Type("int", false);
                    paramsOllir.add(String.format("%s%s", child.get("value"), OllirTemplates.type(type)));
                    params.add(type);
                    break;
                case "Boolean":
                    type = new Type("boolean", false);
                    paramsOllir.add(String.format("%s%s", child.get("value"), OllirTemplates.type(type)));
                    params.add(type);
                    break;
                case "Identifier":
                    List<Object> variable = visit(child, Arrays.asList("PARAM"));

                    statements = ((String) variable.get(0)).split("\n");
                    if (statements.length > 1) {
                        for (int i = 0; i < statements.length - 1; i++) {
                            ollir.append(statements[i]).append("\n");
                        }
                    }

                    params.add(((Symbol) variable.get(1)).getType());
                    paramsOllir.add(statements[statements.length - 1]);
                    break;
                case "ArrayAccess":
                    List<Object> accessExpression = visit(child, Arrays.asList("PARAM"));
                    statements = ((String) accessExpression.get(0)).split("\n");
                    if (statements.length > 1) {
                        for (int i = 0; i < statements.length - 1; i++) {
                            ollir.append(statements[i]).append("\n");
                        }
                    }
                    ollir.append(String.format("%s%s :=%s %s;\n",
                            "temporary" + temp_sequence,
                            OllirTemplates.type((Type) accessExpression.get(1)),
                            OllirTemplates.type((Type) accessExpression.get(1)),
                            statements[statements.length - 1]));

                    paramsOllir.add("temporary" + temp_sequence++ + OllirTemplates.type((Type) accessExpression.get(1)));

                    params.add((Type) accessExpression.get(1));
                    break;
                case "BinaryOp"://TODO: falta fazer para o AND e "<", pois o resultado e boolean e
                    var = (String) visit(child, Arrays.asList("PARAM")).get(0);
                    statements = var.split("\n");
                    result = binaryOperations(statements, ollir, new Type("int", false));
                    params.add(new Type("int", false));

                    paramsOllir.add(result);
                    break;
                case "MethodCall":
                    //currentCallMethodName=functionName;
                    paramsOllir.add((String) visit(child, Collections.singletonList("PARAM")).get(0));
                    break;
                default:
                    break;
            }
        }
        return Map.entry(params, String.join(", ", paramsOllir));
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
