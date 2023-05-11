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

    private int if_sequence = 1;

    private int while_sequence = 1;

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
        this.addVisit("This", this::dealWithThis);
        this.addVisit("Assignment", this::dealWithAssignment);
        this.addVisit("ArrayAssignment", this::dealWithAssignment);
        this.addVisit("Integer", this::dealWithType);
        this.addVisit("Boolean", this::dealWithType);
        this.addVisit("GeneralDeclaration", this::dealWithObjectInit);
        this.addVisit("IntArrayDeclaration",this::dealWithArrayDeclaration);

        this.addVisit("BinaryOp", this::dealWithBinaryOperation);
        this.addVisit("MethodCall", this::dealWithMethodCall);
        this.addVisit("LengthOp",this::dealWithMethodCall);
        this.addVisit("IfElseStmt", this::dealWithIfStatement);
        this.addVisit("WhileStmt",this::dealWithWhileStatement);

        this.addVisit("ArrayAccess", this::dealWithMethodCall);

        this.addVisit("UnaryOp",this::dealWithUnaryOperation);



        setDefaultVisit(this::defaultVisit);
    }

    private List<Object> defaultVisit(JmmNode node, List<Object> data) {
        StringBuilder ollir = new StringBuilder(node.getKind());
        ollir.append(" DEFAULT_VISIT 1");
        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithUnaryOperation(JmmNode node,List<Object> data){
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 20");
        visited.add(node);

        StringBuilder ollir = new StringBuilder("!.bool ");
        String ollirChild = (String) visit(node.getChildren().get(0), Collections.singletonList("UNARY")).get(0);

        ollir.append(ollirChild);


        return Collections.singletonList(ollir.toString());
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

        if (node.getKind().equals("MainMethod")) builder.append("\n").append(OllirTemplates.ret(currentMethod.getReturnType(),""));

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
        System.out.println("visiting ArrayAssignment");

        // ARRAY ACCESS
        if ( node.getKind().equals("ArrayAssignment")) {
            JmmNode indexNode = node.getChildren().get(0);
            Map.Entry<Symbol, Boolean> indexVariable;
            boolean indexClassField = false;

            if ((indexVariable = currentMethod.getField(node.get("name"))) == null) {
                indexVariable = table.getField(node.get("name"));
                indexClassField = true;
            }
            String indexName = !classField ? currentMethod.isParameter(indexVariable.getKey()) : null;
            List<Object> indexVisitResult = visit(indexNode, Arrays.asList(indexClassField ? "FIELD" : "ASSIGNMENT", indexVariable.getKey(), "ARRAY_ACCESS"));
            System.out.println("indexvisitresult is :" +indexVisitResult);
            String indexResult = (String) indexVisitResult.get(0);

            visitResult = visit(node.getChildren().get(1), Arrays.asList(classField ? "FIELD" : "ASSIGNMENT", variable.getKey(), "ARRAY_ACCESS"));
            String target = (String) visitResult.get(0);
            System.out.println("target is :" +target);


            if (!classField) {
                System.out.println("ESTOU AQUI");
                String temp = "temporary" + temp_sequence++ + ".i32";
                ollir.append(String.format("%s :=.i32 %s;\n", temp, indexVisitResult.get(0)));

                System.out.println("Var variable is :" + variable.getKey());
                System.out.println("Var indexName is :" + indexName);
                System.out.println("Var name is :" + name);
                System.out.println("Var result is :" + target);
                System.out.println("Var temp is :" + temp);

                ollir.append(String.format("%s :=%s %s;\n",
                        OllirTemplates.arrayaccess(new Symbol(new Type("int", true),node.get("name")),name,temp),
                        OllirTemplates.type(new Type(variable.getKey().getType().getName(), false)),
                        target));

                /*ollir.append(String.format("%s :=%s %s;\n",
                        OllirTemplates.arrayaccess(new Symbol(new Type("int", true), temp), null, indexResult),
                        OllirTemplates.type(new Type(variable.getKey().getType().getName(), false)),
                        target));*/
            } else {
                String temp = "temporary" + temp_sequence++;

                ollir.append(String.format("%s :=%s %s;\n", temp + ollirType, ollirType, OllirTemplates.getfield(variable.getKey())));

                ollir.append(String.format("%s :=%s %s;\n",
                        OllirTemplates.arrayaccess(new Symbol(new Type("int", true), temp), null, indexResult),
                        OllirTemplates.type(new Type(variable.getKey().getType().getName(), false)),
                        indexResult));
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
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 20");
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

        if( node.get("op").equals("&&") ){
            leftSide = binaryOperations(leftStmts, ollir, new Type("boolean", false));
            rightSide = binaryOperations(rightStmts, ollir, new Type("boolean", false));
        }else{
            leftSide = binaryOperations(leftStmts, ollir, new Type("int", false));
            rightSide = binaryOperations(rightStmts, ollir, new Type("int", false));
        }


        if (data == null) {
            return Arrays.asList("DEFAULT_VISIT 8");
        }
        if (data.get(0).equals("RETURN") || data.get(0).equals("FIELD")) {
            if(node.get("op").equals("&&") ){
                Symbol variable = new Symbol(new Type("boolean", false), "temporary" + temp_sequence++);
                ollir.append(String.format("%s :=.bool %s %s.bool %s;\n", OllirTemplates.variable(variable), leftSide, node.get("op"), rightSide));
                ollir.append(OllirTemplates.variable(variable));
            }else{
                Symbol variable = new Symbol(new Type("int", false), "temporary" + temp_sequence++);
                ollir.append(String.format("%s :=.i32 %s %s.i32 %s;\n", OllirTemplates.variable(variable), leftSide, node.get("op"), rightSide));
                ollir.append(OllirTemplates.variable(variable));
            }

        } else {
            if(node.get("op").equals("&&") ){
                ollir.append(String.format("%s %s.bool %s", leftSide, node.get("op"), rightSide));
            }else {
                ollir.append(String.format("%s %s.i32 %s", leftSide, node.get("op"), rightSide));
            }
        }

        return Collections.singletonList(ollir.toString());
    }


    private List<Object> dealWithVariable(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 9");
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
        return Arrays.asList("ACCESS", node.get("value"));
    }

    private List<Object> dealWithReturn(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 10");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        if(node.getNumChildren() ==1 ){
            if(node.getChildren().get(0).getKind().equals("This")){
                ollir.append(OllirTemplates.ret(currentMethod.getReturnType(), ("this." + currentMethod.getReturnType().getName() )));
                return Collections.singletonList(ollir.toString());
            }
        }

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
        return Arrays.asList("ACCESS", "this");
    }

    private  List<Object> dealWithExpression(JmmNode node, List<Object> data){
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 11");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();
        for (JmmNode child : node.getChildren()){
            String ollirChild = (String) visit(child, Collections.singletonList("EXPR_STMT")).get(0);
            ollir.append(ollirChild);
        }
        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithMethodCall(JmmNode node, List<Object> data) {//TODO: fix when first child is general declaration :)))))
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 12");
        visited.add(node);

        System.out.println("at start of method call current temp counter is :" + temp_sequence );

        String methodClass;
        StringBuilder ollir = new StringBuilder();
        JmmNode targetNode = node.getChildren().get(0);
        JmmNode methodNode = node;

        List<Object> targetReturn = visit(targetNode, Arrays.asList("ACCESS", ollir));
        List<JmmNode> children = node.getChildren();

        children.remove(0);//remove first node as it isnt a parameter

        Map.Entry<List<Type>, String> params = getParametersList(children, ollir);

        String methodString;
        if ( methodNode.getKind().equals("LengthOp") || methodNode.getKind().equals("ArrayAccess") ) methodString = "";
        else methodString = methodNode.get("value");

        if (params.getKey().size() > 0) {
            for (Type param : params.getKey()) {
                methodString += "::" + param.getName() + ":" + (param.isArray() ? "true" : "false");
            }
        }
        System.out.println("methodString is :" + methodString );
        System.out.println("params.getvalue is :"  + params.getValue());
        Type returnType = table.getReturnType(methodString);
        JmmMethod method;

        //ver se identifier é  ou classe do proprio ou objeto da classe proprio
        String targetName;

        if (targetNode.getKind().equals("GeneralDeclaration")){
            targetName = targetNode.get("name");
        }else if (targetNode.getKind().equals("This")){
            targetName = table.getClassName();
        }else{
            targetName = targetNode.get("value");
        }

        if (methodNode.getKind().equals("LengthOp")  || methodNode.getKind().equals("ArrayAccess")) method = null;
        else method = table.getMethod(methodNode.get("value"));

        methodClass = "class_method";

        String identifierType = "";//this might not be safe
        if (currentMethod.fieldExists(targetName) || targetName.equals("This")){
            identifierType = currentMethod.getField(targetName).getKey().getType().getName();
        }

        for( var importName : table.getImports() ){
            if(targetName.equals("This")) break;
            if (!targetName.equals(importName) && !identifierType.equals(importName)) continue;
            if(targetName.equals(table.getClassName())) break;
            methodClass = "method";
        }

        if (methodNode.getKind().equals("LengthOp")) methodClass = "length";
        //###########################################################

        Symbol assignment = (data.get(0).equals("ASSIGNMENT")) ? (Symbol) data.get(1) : null;

        String ollirExpression = null;
        Type expectedType = (data.get(0).equals("BINARY") || (data.size() > 2 && data.get(2).equals("ARRAY_ACCESS"))) ? new Type("int", false) : null;


        if (targetReturn.get(0).equals("ACCESS")) {
            // Static Imported Methods
            if (!targetReturn.get(1).equals("this")) {

                String targetVariable = (String) targetReturn.get(1);
                if (assignment != null) {
                    if (data.get(2).equals("ARRAY_ACCESS")) { //TODO: Fix since changes
                        ollirExpression = OllirTemplates.invokestatic(targetVariable, methodNode.get("value"), new Type(assignment.getType().getName(), false), params.getValue());
                        expectedType = new Type(assignment.getType().getName(), false);
                    } else {
                        ollirExpression = OllirTemplates.invokestatic(targetVariable,  methodNode.get("value"), assignment.getType(),  params.getValue());
                        expectedType = assignment.getType();
                    }
                } else {
                    if(expectedType == null){
                        JmmNode parentNode =methodNode.getJmmParent();
                        if(parentNode.getKind().equals("MethodCall") && table.methodExists(parentNode.get("value"))){

                            expectedType = table.getMethod(parentNode.get("value")).getParameters().get(methodNode.getIndexOfSelf() - 1).getType();
                        }else expectedType = new Type("void", false);
                    }
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
            Symbol array = (Symbol) targetReturn.get(1);
            //String index = (String) visit(methodNode.getChildren().get(1)).get(0);

            //String index = (String) visit(node.getChildren().get(1), Arrays.asList(data.get(0),array,"ARRAY_ACCESS")).get(0);

            /*String[] parts = index.split("\n");
            if (parts.length > 1) {
                for (int i = 0; i < parts.length - 1; i++) {
                    ollir.append(parts[i]).append("\n");
                }
            }*/
            System.out.println("INSIDE ARRAY ACCESS OF METHOD CALL");
            System.out.println("var array is " + array);
            //System.out.println("var index is " + index);


            ollirExpression = OllirTemplates.arrayaccess(array, (String) targetReturn.get(2), params.getValue());
            expectedType = new Type(array.getType().getName(), false);
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
                                methodNode.get("value"),
                                assignment.getType(),
                                params.getValue()
                        );
                        expectedType = assignment.getType();
                    } else {
                        expectedType = (expectedType == null) ? new Type("void", false) : expectedType;
                        ollirExpression = OllirTemplates.invokespecial(
                                OllirTemplates.variable(auxiliary),
                                methodNode.get("value"),
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

                if (methodClass.equals("method")) {//TODO:? this used to be invokespecial, might not be correct this way or need more cases

                    if (assignment != null) {
                        ollirExpression = OllirTemplates.invokevirtual(OllirTemplates.variable((Symbol) targetReturn.get(1)),  methodNode.get("value"), assignment.getType(), params.getValue());
                        expectedType = assignment.getType();
                    } else {
                        expectedType = (expectedType == null) ? new Type("void", false) : expectedType;
                        ollirExpression = OllirTemplates.invokevirtual(OllirTemplates.variable((Symbol) targetReturn.get(1)), params.getValue(), expectedType,  params.getValue());
                    }
                } else if (!methodClass.equals("length")) {
                    Symbol targetVariable = (Symbol) targetReturn.get(1);

                    ollirExpression = OllirTemplates.invokevirtual(OllirTemplates.variable(targetVariable), method.getName(), method.getReturnType(), params.getValue());
                    expectedType = method.getReturnType();
                }else {
                    ollirExpression = OllirTemplates.arraylength(OllirTemplates.variable((Symbol) targetReturn.get(1), (String) targetReturn.get(2)));
                    expectedType = new Type("int", false);
                }
            }
        }

        //TODO: REVER CONDICOES EM BAIXO
        if ((data.get(0).equals("CONDITION") || data.get(0).equals("BINARY") || data.get(0).equals("FIELD") || data.get(0).equals("PARAM") || data.get(0).equals("RETURN")) && expectedType != null && ollirExpression != null) {
            Symbol auxiliary = new Symbol(expectedType, "temporary" + temp_sequence++);
            ollir.append(String.format("%s :=%s %s;\n", OllirTemplates.variable(auxiliary), OllirTemplates.type(expectedType), ollirExpression));//TODO: problema parece estar aqui

            if (data.get(0).equals("CONDITION")) {
                ollir.append(String.format("%s ==.bool 1.bool", OllirTemplates.variable(auxiliary)));
            }else if (data.get(0).equals("BINARY") || data.get(0).equals("FIELD") || data.get(0).equals("PARAM") || data.get(0).equals("RETURN")) {

                if (methodNode.getJmmParent().getKind().equals("MethodCall")){
                    //ollir.append(ollirExpression);

                } else ollir.append(String.format("%s", OllirTemplates.variable(auxiliary)));
            }
        }else {
            ollir.append(ollirExpression);
        }


        if (data.get(0).equals("EXPR_STMT")||data.get(0).equals("METHOD") || data.get(0).equals("IF") || data.get(0).equals("ELSE") || data.get(0).equals("WHILE")) {
            ollir.append(";");
        }

        System.out.println("at end of method call current temp counter is :" + temp_sequence );
        System.out.println("at end of method call current ollir is :" + ollir);

        if (methodNode.getJmmParent().getKind().equals("MethodCall") || methodNode.getJmmParent().getKind().equals("ArrayAccess")  && data.get(0).equals("PARAM")){
            return Arrays.asList(ollir.toString(),expectedType,"temporary" + (temp_sequence - 1),"PARAM");
        }
        return Arrays.asList(ollir.toString(), expectedType);


    }

    private List<Object> dealWithArrayDeclaration(JmmNode node, List<Object> data){
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 13");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        String size = (String) visit(node.getChildren().get(0), Collections.singletonList("RETURN")).get(0);

        String[] sizeParts = size.split("\n");
        if (sizeParts.length > 1) {
            for (int i = 0; i < sizeParts.length - 1; i++) {
                ollir.append(sizeParts[i]).append("\n");
            }
        }

        ollir.append(OllirTemplates.arrayinit(sizeParts[sizeParts.length - 1]));

        return Arrays.asList(ollir.toString(), "ARRAY_INIT");
    }

    private List<Object> dealWithArrayAccess(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        /*String target = (String) visit(node.getChildren().get(0)).get(0);
            String[] parts = target.split("\n");
            if (parts.length > 1) {
                for (int i = 0; i < parts.length - 1; i++) {
                    ollir.append(parts[i]).append("\n");
                }
            }*/

        //visitResult = visit(node.getChildren().get(0).getChildren().get(1), Arrays.asList(classField ? "FIELD" : "ASSIGNMENT", variable.getKey(), "ARRAY_ACCESS"));

        String targetVisitResult  = (String) visit(node.getChildren().get(0), Arrays.asList("RETURN")).get(0);
        String indexVisitResult = (String) visit(node.getChildren().get(1), Arrays.asList(data.get(0),data.get(1),"ARRAY_ACCESS")).get(0);

        //String result = (String) visitResult.get(0);

        String ollir =  OllirTemplates.arrayaccess(new Symbol(new Type("int", true), indexVisitResult), null, targetVisitResult);

        System.out.println("ON ARRAY ACCESS VISIT");
        System.out.println("ollir built on visit is" + ollir);

        //String visit = (String) visit(node.getChildren().get(0), Arrays.asList("RETURN")).get(0);

        return Arrays.asList(targetVisitResult);
    }

    private List<Object> dealWithIfStatement(JmmNode node, List<Object> data){
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 14");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        JmmNode ifConditionNode = node.getChildren().get(0);
        JmmNode ifCodeNode = node.getChildren().get(1);
        JmmNode elseCodeNode = node.getChildren().get(2);

        int count = if_sequence++;

        String ifCondition = (String) visit(ifConditionNode, Collections.singletonList("CONDITION")).get(0);

        String[] ifConditionParts = ifCondition.split("\n");
        if (ifConditionParts.length > 1) {
            for (int i = 0; i < ifConditionParts.length - 1; i++) {
                ollir.append(ifConditionParts[i]).append("\n");
            }
        }

        if (ifConditionParts[ifConditionParts.length - 1].contains("==.bool 1.bool")) {
            String condition = ifConditionParts[ifConditionParts.length - 1].split(" ==.bool ")[0];
            ollir.append(String.format("if (%s !.bool %s) goto else%d;\n", condition, condition, count));
        } else {
            Symbol aux = new Symbol(new Type("boolean", false), "temporary" + temp_sequence++);
            ollir.append(String.format("%s :=.bool %s;\n", OllirTemplates.variable(aux), ifConditionParts[ifConditionParts.length - 1]));
            ollir.append(String.format("if (%s !.bool %s) goto else%d;\n", OllirTemplates.variable(aux), OllirTemplates.variable(aux), count));
        }

        List<String> ifBody = new ArrayList<>();

        for (JmmNode child : ifCodeNode.getChildren()){
            ifBody.add((String) visit(child, Collections.singletonList("IF")).get(0));
        }
        ollir.append(String.join("\n", ifBody)).append("\n");
        ollir.append(String.format("goto endif%d;\n", count));

        ollir.append(String.format("else%d:\n", count));

        List<String> elseBody = new ArrayList<>();

        for (JmmNode child : elseCodeNode.getChildren()){
            elseBody.add((String) visit(child, Collections.singletonList("IF")).get(0));
        }

        ollir.append(String.join("\n", elseBody)).append("\n");

        ollir.append(String.format("endif%d:", count));

        return Collections.singletonList(ollir.toString());
    }

    private List<Object> dealWithWhileStatement(JmmNode node, List<Object> data){
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT 14");
        visited.add(node);

        StringBuilder ollir = new StringBuilder();

        JmmNode ConditionNode = node.getChildren().get(0);
        JmmNode codeNode = node.getChildren().get(1);

        int count = while_sequence++;

        ollir.append(String.format("loop%d:\n", count));

        String condition = (String) visit(ConditionNode, Collections.singletonList("WHILE")).get(0);
        String[] conditionParts = condition.split("\n");
        if (conditionParts.length > 1) {
            for (int i = 0; i < conditionParts.length - 1; i++) {
                ollir.append(conditionParts[i]).append("\n");
            }
        }

        if (conditionParts[conditionParts.length - 1].contains("==.bool 1.bool")) {
            String conditionAux = conditionParts[conditionParts.length - 1].split(" ==.bool ")[0];
            ollir.append(String.format("if (%s !.bool %s) goto endloop%d;\n", conditionAux, conditionAux, count));
        } else {
            Symbol aux = new Symbol(new Type("boolean", false), "temporary" + temp_sequence++);
            ollir.append(String.format("%s :=.bool %s;\n", OllirTemplates.variable(aux), conditionParts[conditionParts.length - 1]));
            ollir.append(String.format("if (%s !.bool %s) goto endloop%d;\n", OllirTemplates.variable(aux), OllirTemplates.variable(aux), count));
        }

        List<String> body = new ArrayList<>();
        for (JmmNode child : codeNode.getChildren()){
            body.add((String) visit(child, Collections.singletonList("IF")).get(0));
        }
        ollir.append(String.join("\n", body)).append("\n");

        ollir.append(String.format("goto loop%d;\n", count));

        ollir.append(String.format("endloop%d:", count));

        return Collections.singletonList(ollir.toString());
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
                /*case "ArrayAccess":
                    List<Object> accessExpression = visit(child, Arrays.asList("PARAM"));
                    statements = ((String) accessExpression.get(0)).split("\n");
                    if (statements.length > 1) {
                        for (int i = 0; i < statements.length - 1; i++) {
                            ollir.append(statements[i]).append("\n");
                        }
                    }
                    ollir.append(String.format("%s%s :=%s %s;\n",//TODO: PROBLEMA AQUI
                            "testtemporary" + temp_sequence,
                            OllirTemplates.type((Type) accessExpression.get(1)),
                            OllirTemplates.type((Type) accessExpression.get(1)),
                            statements[statements.length - 1]));

                    paramsOllir.add("temporary" + temp_sequence++ + OllirTemplates.type((Type) accessExpression.get(1)));

                    params.add((Type) accessExpression.get(1));
                    break;*/
                case "BinaryOp":
                    var = (String) visit(child, Arrays.asList("PARAM")).get(0);
                    statements = var.split("\n");//TODO: falta testar :upside_down:
                    if ( child.get("op").equals("<")  || child.get("op").equals("&&") ){
                        result = binaryOperations(statements, ollir, new Type("boolean", false));
                        params.add(new Type("boolean", false));
                    }else{
                        result = binaryOperations(statements, ollir, new Type("int", false));
                        params.add(new Type("int", false));
                    }


                    paramsOllir.add(result);
                    break;
                case "ArrayAccess":
                case "MethodCall":
                case "LengthOp":
                    List<Object> methodCallVisitResult = visit(child, Collections.singletonList("PARAM"));

                    ollir.append((String) methodCallVisitResult.get(0));
                    paramsOllir.add( methodCallVisitResult.get(2) + OllirTemplates.type((Type) methodCallVisitResult.get(1) ));//adicionar variavel temp com o respetivo tipo a lista de param
                    params.add((Type) methodCallVisitResult.get(1));

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
