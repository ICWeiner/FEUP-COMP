package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SemanticAnalysisVisitor extends AJmmVisitor<Boolean, Boolean> {
    private final SymbolTable table;
    private final List<Report> reports;
    String currentMethodName;

    public SemanticAnalysisVisitor(SymbolTable table, List<Report> reports) {
        this.table = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        this.setDefaultVisit(this::dealWithDefault);
        this.addVisit("Program", this::dealWithProgram);
        this.addVisit("BinaryOp", this::dealWithBinaryOp);
        this.addVisit("IfElseStmt", this::dealWithConditionalStmt);
        this.addVisit("WhileStmt", this::dealWithConditionalStmt);
        this.addVisit("ArrayAccess", this::dealWithArrayAccess);
        this.addVisit("Assignment", this::dealWithAssignment);
        this.addVisit("ArrayAssignment", this::dealWithArrayAssignment);
        this.addVisit("MethodCall", this::dealWithMethodCall);
        this.addVisit("Identifier", this::dealWithIdentifier);
        this.addVisit("MethodDeclaration", this::dealWithMethod);
        this.addVisit("ReturnDeclaration", this::dealWithReturn);
    }

    private Boolean dealWithDefault(JmmNode node, Boolean data) {
        System.out.println("Default: " + node);
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return true;
    }

    private Boolean dealWithProgram(JmmNode node, Boolean data){
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return true;
    }

    private Boolean dealWithMethod(JmmNode node, Boolean data){
        System.out.println("Method: " + node + node.getChildren());

        if (node.getKind().equals("MainMethod")) {
            currentMethodName = "main";
            for (JmmNode child : node.getChildren()) {
                visit(child);
            }
            return true;
        }
        else {
            List<String> methods = table.getMethods();

            if(methods.contains(node.getJmmChild(0).get("name"))) {
                currentMethodName = node.getJmmChild(0).get("name");
            }

            for (JmmNode child : node.getChildren()) {
                visit(child);
            }
        }
        return true;
    }

    private Boolean dealWithReturn(JmmNode node, Boolean data){
        System.out.println("Return: " + node.getChildren() + " " + node.getJmmParent());

        Type nodeType = table.getReturnType(currentMethodName);
        if(nodeType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Assignment variable type is null"));
            return false;
        }

        JmmNode child = node.getJmmChild(0);
        String superClassName = table.getSuper();
        String className = table.getClassName();
        List<String> imports = table.getImports();
        if(!child.getKind().equals("Identifier")) {
            //TODO é provavel que estas condições não estejam bem 💀
            if(child.getKind().equals("MethodCall")) {
                if(!visit(child,true)) return false;
                if(!((table.getReturnType(child.get("value")) == null && !imports.isEmpty()) //TODO falta verificar mais coisas para alem dos imports?
                    || (table.getReturnType(child.get("value")) != null && table.getReturnType(currentMethodName).getName().equals(table.getReturnType(child.get("value")).getName())))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Method Call: Incompatible Return in " + currentMethodName + " method"));  //TODO as mensagens dos reports não estão muito bem
                }
            }
            if(!(nodeType.isArray() && nodeType.getName().equals("int") && child.getKind().equals("IntArrayDeclaration")
                    && (child.getJmmChild(0).getKind().equals("Integer")
                    || (table.getVariableType(child.getJmmChild(0).get("value"),currentMethodName) != null && table.getVariableType(child.getJmmChild(0).get("value"),currentMethodName).getName().equals("int"))))

                    && !(!nodeType.isArray() && child.getKind().equals("Integer") && nodeType.getName().equals("int"))
                    && !(child.getKind().equals("Boolean") && nodeType.getName().equals("boolean"))
                    && !(child.getKind().equals("GeneralDeclaration") && nodeType.getName().equals(child.get("name"))) //TODO acho que isto não está bem
                    && !((child.getKind().equals("BinaryOp") && ((child.get("op").equals("&&") && nodeType.getName().equalsIgnoreCase("boolean") && table.getReturnType(currentMethodName).getName().equals("boolean")) || (!child.get("op").equals("&&") && nodeType.getName().equals("int") && table.getReturnType(currentMethodName).getName().equals("int"))) && visit(child,true))) ////TODO AAAA
                    && !(child.getKind().equals("ArrayAccess") && visit(child,true) && table.getReturnType(currentMethodName).getName().equals("int"))
                    && child.getKind().equals("This") && !table.getReturnType(currentMethodName).getName().equals(className) //TODO
                    && !(child.getKind().equals("LengthOp") && nodeType.isArray() && table.getReturnType(currentMethodName).getName().equals("int"))) {
                if(reports.isEmpty()) reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Incompatible return in " + currentMethodName + " method: " + child.getKind() + " and " + nodeType.getName()));  //TODO as mensagens dos reports não estão muito bem
                return false;
            }
        }
        else {
            Type childType = table.getVariableType(child.get("value"),currentMethodName);
            if(childType == null) {
                if (child.getKind().equals("Identifier")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Return is null"));
                    return false;
                }
                return true;
            }

            if (!childType.getName().equals(nodeType.getName())) {
                if (!((className.equals(childType.getName()) && superClassName != null && superClassName.equals(nodeType.getName()) && imports.contains(nodeType.getName()))
                        || (className.equals(nodeType.getName()) && imports.contains(childType.getName()))
                        || (imports.contains(nodeType.getName()) && imports.contains(childType.getName())))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Incompatible return: " + nodeType.getName() + " to " + childType.getName()));
                    return false;
                }
            }
        }

        return true;
    }

    private Boolean dealWithMethodCall(JmmNode node, Boolean data){
        System.out.println("MethodCall: " + node + " " + node.getChildren());

        if(node.getChildren().size() > 1) {
            if(!visit(node.getJmmChild(1),true)) {
                return false;
            }
        }

        JmmNode leftChild = node.getJmmChild(0);
        Type leftChildType = null;

        if(!leftChild.getKind().equals("This") && !leftChild.getKind().equals("GeneralDeclaration")) {
            leftChildType = table.getVariableType(leftChild.get("value"),currentMethodName);
        }

        List<String> imports = table.getImports();
        if(leftChildType == null) {
            if (leftChild.getKind().equals("This")) {
                if(currentMethodName.equals("main")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: 'this' invoked in main method"));
                    return false;
                }
                leftChildType = new Type(table.getClassName(), false);
            }
            else if (leftChild.getKind().equals("GeneralDeclaration")) {
                if (!(table.getClassName().contains(leftChild.get("name")) || table.getImports().contains(leftChild.get("name")) || (table.getSuper() != null && table.getSuper().contains(leftChild.get("name"))))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Undeclared class " + leftChild.get("name")));
                    return false;
                }
                leftChildType = new Type(leftChild.get("name"), false);
            }
            else if (!table.getClassName().equals(leftChild.get("value")) && !imports.contains(leftChild.get("value"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Method Call: Class not imported"));
                return false;
            }
            else {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Method Call: Left type is null"));
                return false;
            }
        }

        List<String> methods = table.getMethods();
        String superClassName = table.getSuper();
        if(!(superClassName != null && imports.contains(superClassName))
                && !imports.contains(leftChildType.getName())
                && !methods.contains(node.get("value"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Call to undeclared method"));
            return false;
        }

        if(!imports.contains(leftChildType.getName()) && methods.contains(node.get("value"))) {
            List<Symbol> parameters = table.getParameters(node.get("value"));
            if((node.getChildren().size()-1 != parameters.size())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Wrong number of parameters"));
                return false;
            }
            Type childType;
            for(JmmNode child : node.getChildren()) {
                if(child.getIndexOfSelf() != 0) {
                    if(child.getKind().equals("Identifier")) {
                        childType = table.getVariableType(child.get("value"), currentMethodName);
                    }
                    else if (child.getKind().equals("Integer")) {
                        childType = new Type("int",false);
                    }
                    else if(child.getKind().equals("MethodCall")) {
                        if(imports.contains(child.getJmmChild(0).get("value"))) return true;
                        if(!visit(child,true)) { //TODO
                            if(reports.isEmpty()) reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Incompatible arguments"));
                            return false;
                        }
                        childType = new Type(table.getReturnType(child.get("value")).getName(), false);
                    }
                    else if (leftChild.getKind().equals("This")) {
                        if(currentMethodName.equals("main")) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: 'this' invoked in main method"));
                            return false;
                        }
                        childType = new Type(table.getClassName(), false);
                    }
                    else if(child.getKind().equals("ArrayAccess")) {
                        if(!visit(child,true)) return false; //TODO
                        childType = new Type("int",false);
                    }
                    else {
                        childType = new Type(child.getKind(),false);
                    }
                    if(childType == null) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Variable not declared"));
                        return false;
                    }
                    if(!parameters.isEmpty()) {
                        Type parameterType = parameters.get(child.getIndexOfSelf()-1).getType();
                        if(!parameterType.getName().equalsIgnoreCase(childType.getName()) || !(parameterType.isArray() == childType.isArray())) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Incompatible arguments"));
                            return false;
                        }
                    }
                    else {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Incompatible arguments"));
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private Boolean dealWithIdentifier(JmmNode node, Boolean data) {
        System.out.println("Identifier: " + node);
        Type nodeType = table.getVariableType(node.get("value"),currentMethodName);
        if(nodeType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Variable not declared"));
            return false;
        }
        return true;
    }

    private Boolean dealWithArrayAssignment(JmmNode node, Boolean data){
        System.out.println("ArrayAssignment: " + node + " " + node.getChildren());

        Type nodeType = table.getVariableType(node.get("name"),currentMethodName);
        if(nodeType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array assignment is null"));
            return false;
        }
        else if(!nodeType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array assignment is not an array"));
            return false;
        }

        JmmNode leftChild = node.getJmmChild(0);
        if(leftChild.getKind().equals("BinaryOp") && (leftChild.get("op").equals("<") || leftChild.get("op").equals("&&") || !visit(leftChild,true))) { //TODO
            if(reports.isEmpty()) reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array index is not an integer"));
            return false;
        }
        else if(leftChild.getKind().equals("Identifier")) {
            Type leftChildType = table.getVariableType(leftChild.get("value"),currentMethodName);
            if(leftChildType == null || !leftChildType.getName().equals("int")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array index is not an integer"));
                return false;
            }
        }
        else if(leftChild.getKind().equals("MethodCall") && !table.getImports().contains(leftChild.getJmmChild(0).get("value")) && !table.getReturnType(currentMethodName).getName().equals("int") && !visit(leftChild,true)) { //TODO
            if(reports.isEmpty()) reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array index is not an integer"));
            return false;
        }
        else if(leftChild.getKind().equals("ArrayAccess") && !visit(leftChild,true)) { //TODO
            return false;
        }
        else if(!leftChild.getKind().equals("ArrayAccess") && !leftChild.getKind().equals("MethodCall") && !leftChild.getKind().equals("BinaryOp") && !leftChild.getKind().equals("Integer")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array index is not an integer"));
            return false;
        }

        JmmNode rightChild = node.getJmmChild(1);
        if(rightChild.getKind().equals("BinaryOp") && (rightChild.get("op").equals("&&") || !visit(rightChild,true))) {
            return false;
        }
        else if(!rightChild.getKind().equals("BinaryOp") && !rightChild.getKind().equals("Integer")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array assignment is not an integer"));
            return false;
        }

        return true;
    }

    private Boolean dealWithAssignment(JmmNode node, Boolean data){
        System.out.println("Assignment: " + node + " " + node.getChildren());

        Type nodeType = table.getVariableType(node.get("name"),currentMethodName);
        if(nodeType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Assignment variable type is null"));
            return false;
        }

        JmmNode child = node.getJmmChild(0);
        String superClassName = table.getSuper();
        String className = table.getClassName();
        if(!child.getKind().equals("Identifier")) {
            if(currentMethodName.equals("main") && table.getFields().contains(new Symbol(nodeType,node.get("name")))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Field in static"));
                return false;
            }
            //TODO é provavel que estas condições não estejam bem 💀
            if(!(nodeType.isArray() && nodeType.getName().equals("int") && child.getKind().equals("IntArrayDeclaration")
                    && (child.getJmmChild(0).getKind().equals("Integer")
                    || (table.getVariableType(child.getJmmChild(0).get("value"),currentMethodName) != null && table.getVariableType(child.getJmmChild(0).get("value"),currentMethodName).getName().equals("int"))))

                    && !(!nodeType.isArray() && nodeType.getName().equals("int") && child.getKind().equals("Integer"))
                    && !(child.getKind().equals("Boolean") && nodeType.getName().equals("boolean"))
                    && !(child.getKind().equals("GeneralDeclaration") && nodeType.getName().equals(child.get("name"))) //TODO acho que isto não está bem
                    && !((child.getKind().equals("BinaryOp") && (child.get("op").equals("&&") && nodeType.getName().equalsIgnoreCase("boolean") || (!child.get("op").equals("&&") && nodeType.getName().equals("int"))) && visit(child,true)))
                    && !(child.getKind().equals("MethodCall") && visit(child,true))
                    && !(!nodeType.isArray() && nodeType.getName().equals("int") && child.getKind().equals("ArrayAccess") && visit(child,true))
                    && !(child.getKind().equals("LengthOp") && nodeType.isArray())
                    && !(child.getKind().equals("This") && !currentMethodName.equals("main") && ((superClassName != null && superClassName.equals(nodeType.getName())) || className.equals(nodeType.getName())))) {
                if(reports.isEmpty()) reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Assign " + nodeType.getName() + " to " + child.getKind() + " in " + currentMethodName + " method")); //TODO as mensagens dos reports não estão muito bem
                return false;
            }
        }
        else {
            Type childType = table.getVariableType(child.get("value"),currentMethodName);
            if(childType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Assign is null"));
                return false;
            }
            else if(table.getFields().contains(new Symbol(childType,child.get("value"))) && currentMethodName.equals("main")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Field in static"));
                return false;
            }

            if((nodeType.isArray() && !childType.isArray()) || (!nodeType.isArray() && childType.isArray())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array assignment"));
                return false;
            }

            if (!childType.getName().equals(nodeType.getName())) {
                List<String> imports = table.getImports();
                if (!((className.equals(childType.getName()) && superClassName != null && superClassName.equals(nodeType.getName()) && imports.contains(nodeType.getName()))
                        || (className.equals(nodeType.getName()) && imports.contains(childType.getName()))
                        || (imports.contains(nodeType.getName()) && imports.contains(childType.getName())))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Assign " + nodeType.getName() + " to " + childType.getName()));
                    return false;
                }
            }
        }

        return true;
    }

    private Boolean dealWithConditionalStmt(JmmNode node, Boolean data){
        System.out.println("ConditionalStmt: " + node.getChildren());

        JmmNode child = node.getJmmChild(0);

        if (child.getKind().equals("Identifier")) {
            Type childType = table.getVariableType(child.get("value"),currentMethodName);
            if (childType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Conditional statement is null"));
                return false;
            }
            if (childType.isArray()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Conditional statement is an array"));
                return false;
            }
            if (!childType.getName().equals("boolean")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Conditional statement is not boolean"));
                return false;
            }
        }
        else if(child.getKind().equals("BinaryOp")) {
            if(!visit(child,true)) return false; //TODO
            if(!child.get("op").equals("<") && !child.get("op").equals("&&")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Conditional statement is not boolean"));
                return false;
            }
        }
        else if(child.getKind().equals("MethodCall")) {
            if(!visit(child,true)) return false; //TODO
            if(!child.getJmmChild(0).getKind().equals("This") && !table.getImports().contains(child.getJmmChild(0).get("value")) && !table.getReturnType(currentMethodName).getName().equals("boolean")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Conditional statement is not boolean"));
                return false;
            }
        }
        else if (!child.getKind().equals("Boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Conditional statement is not boolean"));
            return false;
        }

        for(JmmNode bs : node.getChildren()) {
            if(bs.getIndexOfSelf() != 0) {
                if(!visit(bs,true)) return false;
            }
        }
        return true;
    }

    private Boolean dealWithArrayAccess(JmmNode node, Boolean data) {
        System.out.println("ArrayAccess: " + node.getChildren());

        JmmNode array = node.getJmmChild(0);

        if(array.getKind().equals("Identifier")) {
            Type arrayType = table.getVariableType(array.get("value"),currentMethodName);
            if (arrayType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array type is null"));
                return false;
            }
            if(!arrayType.isArray()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array access is not done over an array"));
                return false;
            }
        }
        else {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array access is not done over an array"));
            return false;
        }

        JmmNode index = node.getJmmChild(1);
        if(index.getKind().equals("BinaryOp") && (index.get("op").equals("<") || index.get("op").equals("&&") || !visit(index,true))) { //TODO
            if(reports.isEmpty()) reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array index is not an integer"));
            return false;
        }
        else if(index.getKind().equals("MethodCall") && !table.getReturnType(currentMethodName).getName().equals("int") && !visit(index,true)) { //TODO
            if(reports.isEmpty()) reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array index is not an integer"));
            return false;
        }
        else if(index.getKind().equals("ArrayAccess") && !visit(index,true)) { //TODO
            return false;
        }
        else if(index.getKind().equals("LengthOp") && !table.getVariableType(index.getJmmChild(0).get("value"), currentMethodName).isArray()) { //TODO
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array index is not an integer"));
            return false;
        }
        else if(!index.getKind().equals("BinaryOp") && !index.getKind().equals("ArrayAccess") && !index.getKind().equals("MethodCall") && !index.getKind().equals("LengthOp")) {
            Type indexType = table.getVariableType(index.get("value"), currentMethodName);

            if (!index.getKind().equals("Identifier")) {
                indexType = new Type(index.getKind(), false);
            }
            else if (indexType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array index is null"));
                return false;
            }

            if (indexType.isArray() || (!indexType.getName().equals("int") && !indexType.getName().equals("Integer"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array index is not an integer"));
                return false;
            }

        }
        return true;
    }

    private Boolean dealWithBinaryOp(JmmNode node, Boolean data) {
        System.out.println("BinaryOp: " + node.getJmmChild(0) + " " + node.getJmmChild(1));

        Type leftType = binaryOpChildType(node,node.getJmmChild(0));
        Type rightType = binaryOpChildType(node,node.getJmmChild(1));
        
        if(leftType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: BinaryOp: left type is null"));
            return false;
        }
        if(rightType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: BinaryOp: right type is null"));
            return false;
        }
        if (!node.get("op").equals("&&")) {
            if ((!leftType.getName().equals("int") && !leftType.getName().equals("Integer")) || (!rightType.getName().equals("int") && !rightType.getName().equals("Integer"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Incompatible types in " + node.get("op") + " operation: " + leftType.getName() + " and " + rightType.getName()));
                return false;
            }
        }
        else if (!leftType.getName().equalsIgnoreCase("boolean") || !rightType.getName().equalsIgnoreCase("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Incompatible types in " + node.get("op") + " operation: " + leftType.getName() + " and " + rightType.getName()));
            return false;
        }

        return true;
    }

    ////////////////////////////////////////////////////////////
    ///////////////Semantic Analysis Utils/////////////////////
    //////////////////////////////////////////////////////////

    private Type binaryOpChildType(JmmNode node, JmmNode child) {

        if(child.getKind().equals("ArrayAccess")) {
            if(!visit(child,true)) return null;
            return new Type("int",false);
        }
        else if(child.getKind().equals("BinaryOp")) {
            if(!visit(child,true)) return null;
            if(child.get("op").equals("&&")) return new Type("boolean",false);
            return new Type("int",false);
        }
        else if(child.getKind().equals("MethodCall")) {
            if(!visit(child,true)) return null;
            if(!child.getJmmChild(0).getKind().equals("This") && !table.getImports().contains(child.getJmmChild(0).get("value"))) return new Type(table.getReturnType(child.get("value")).getName(), false);
            if(node.get("op").equals("&&")) return new Type("boolean",false);
            return new Type("int",false);
        }
        else if(child.getKind().equals("LengthOp")) {
            return new Type("int",false);
        }
        else if (child.getKind().equals("Identifier")) {
            Type childType = table.getVariableType(child.get("value"),currentMethodName);
            if (childType != null && childType.isArray()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")), "Error: Array cannot be used in arithmetic operations"));
                return null;
            }
            return childType;
        }

        return new Type(child.getKind(),false);
    }
}
