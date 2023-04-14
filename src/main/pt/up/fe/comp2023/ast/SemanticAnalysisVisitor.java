package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.analysis.table.Type;
import java.util.List;

public class SemanticAnalysisVisitor extends AJmmVisitor<Boolean, Boolean> {
    private final SymbolTable table;
    private final List<Report> reports;

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
        this.addVisit("MethodCall", this::dealWithMethodCall);
        this.addVisit("Identifier", this::dealWithIdentifier);
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

    private Boolean dealWithMethodCall(JmmNode node, Boolean data){
        System.out.println("MethodCall: " + node.getChildren());

        JmmNode leftChild = node.getJmmChild(0);
        Type leftChildType = table.getLocalVariableType(leftChild.get("value"),table.getCurrentMethod().getName());
        List<String> imports = table.getImports();

        if(leftChildType == null) {
            if(!table.getClassName().equals(leftChild.get("value")) && !imports.contains(leftChild.get("value"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Method Call: Class not imported"));
                return false;
            }
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Method Call: Variable not declared"));
            return false;
        }

        List<String> methods = table.getMethods();
        String superClassName = table.getSuper();

        if(!(superClassName != null && imports.contains(superClassName))
                && !imports.contains(leftChildType.getName())
                && !methods.contains(node.get("value"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Call to undeclared method"));
            return false;
        }

        if(methods.contains(node.get("value"))) {
            if(!table.getReturnType(table.getCurrentMethod().getName()).equals(table.getReturnType(node.get("value")))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Incompatible return"));
                return false;
            }
            List<Symbol> parameters = table.getParameters(node.get("value"));
            if(!parameters.isEmpty()) {
                for(Symbol parameter : parameters) {
                    if(parameter.getType().getName().equals(leftChildType.getName())) //só funciona para funções com apenas um argumento
                        return true;
                }
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Incompatible arguments"));
                return false;
            }

        }

        return true;
    }

    private Boolean dealWithIdentifier(JmmNode node, Boolean data) {
        System.out.println("Identifier: " + node);
        Type nodeType = table.getLocalVariableType(node.get("value"),table.getCurrentMethod().getName());
        if(nodeType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Variable not declared"));
            return false;
        }
        return true;
    }

    private Boolean dealWithAssignment(JmmNode node, Boolean data){
        System.out.println("Assignment: " + node + " " + node.getChildren());

        Type nodeType = table.getLocalVariableType(node.get("name"),table.getCurrentMethod().getName());
        if(nodeType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Assignment variable type is null"));
            return false;
        }

        JmmNode child = node.getJmmChild(0);
        String superClassName = table.getSuper();
        String className = table.getClassName();
        if(!child.getKind().equals("Identifier")) {
            if (!(nodeType.isArray() && nodeType.getName().equals("int") && child.getKind().equals("IntArrayDeclaration"))
                    && !(!nodeType.isArray() && nodeType.getName().equals("int") && child.getKind().equals("Integer"))
                    && !(nodeType.getName().equals("boolean") && child.getKind().equals("Boolean"))
                    && !(child.getKind().equals("GeneralDeclaration") && nodeType.getName().equals(child.get("name")))
                    && !(child.getKind().equals("GeneralDeclaration") && nodeType.getName().equals(child.get("name")))
                    && !(child.getKind().equals("This") && !table.getCurrentMethod().getName().equals("main") && (superClassName != null && superClassName.equals(nodeType.getName()) || className.equals(nodeType.getName())))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Assign " + nodeType.getName() + " to " + child.getKind() + " in " + table.getCurrentMethod().getName() + " method"));
                return false;
            }
        }
        else {
            Type childType = table.getLocalVariableType(child.get("value"),table.getCurrentMethod().getName());
            if(childType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Assign is null"));
                return false;
            }

            if (!childType.getName().equals(nodeType.getName())) {
                List<String> imports = table.getImports();
                if (!((className.equals(childType.getName()) && superClassName != null && superClassName.equals(nodeType.getName()) && imports.contains(nodeType.getName()))
                        || (className.equals(nodeType.getName()) && superClassName != null && superClassName.equals(childType.getName()) && imports.contains(childType.getName()))
                        || (imports.contains(nodeType.getName()) && imports.contains(childType.getName())))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Assign " + nodeType.getName() + " to " + childType.getName()));
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
            Type childType = table.getLocalVariableType(child.get("value"),table.getCurrentMethod().getName());
            if (childType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Conditional statement is null"));
                return false;
            }
            if (childType.isArray()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Conditional statement is an array"));
                return false;
            }
            if (!childType.getName().equals("boolean")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Conditional statement is not boolean"));
                return false;
            }
        }
        else if(child.getKind().equals("BinaryOp") && (child.get("op").equals("<") || child.get("op").equals("&&"))) {
            JmmNode left = node.getJmmChild(0).getJmmChild(0);
            JmmNode right = node.getJmmChild(0).getJmmChild(1);

            Type leftType = table.getLocalVariableType(left.get("value"),table.getCurrentMethod().getName());
            Type rightType = table.getLocalVariableType(right.get("value"),table.getCurrentMethod().getName());

            if (leftType == null || rightType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Conditional statement is null"));
                return false;
            }
            if (leftType.isArray() || rightType.isArray()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Conditional statement is an array"));
                return false;
            }
        }
        else if (!child.getKind().equals("Boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Conditional statement not boolean"));
            return false;
        }

        return true;
    }

    private Boolean dealWithArrayAccess(JmmNode node, Boolean data) {
        System.out.println("ArrayAccess: " + node.getChildren());

        JmmNode array = node.getJmmChild(0);

        if(array.getKind().equals("Identifier")) {
            Type arrayType = table.getLocalVariableType(array.get("value"),table.getCurrentMethod().getName());
            if (arrayType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Array type is null"));
                return false;
            }
            if(!arrayType.isArray()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Array access is done over an array"));
                return false;
            }
        }
        else {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Array access is done over an array"));
            return false;
        }

        JmmNode index = node.getJmmChild(1);
        Type indexType = table.getLocalVariableType(index.get("value"),table.getCurrentMethod().getName());

        if (!index.getKind().equals("Identifier")) {
            indexType = new Type(index.getKind(),false);
        }
        else if (indexType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Array index is null"));
            return false;
        }

        if(indexType.isArray() || (!indexType.getName().equals("int") && !indexType.getName().equals("Integer"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Array index is not an int"));
            return false;
        }

        return true;
    }

    private Boolean dealWithBinaryOp(JmmNode node, Boolean data) {
        System.out.println("BinaryOp: " + node.getJmmChild(0) + " " + node.getJmmChild(1));

        if (node.getNumChildren() != 2) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Wrong number of operands"));
            return false;
        }
        JmmNode left = node.getJmmChild(0);
        JmmNode right = node.getJmmChild(1);

        Type leftType = table.getLocalVariableType(left.get("value"),table.getCurrentMethod().getName());
        Type rightType = table.getLocalVariableType(right.get("value"),table.getCurrentMethod().getName());

        if (!left.getKind().equals("Identifier")){
            leftType = new Type(left.getKind(),false);
        }
        else if (leftType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Left type is null"));
            return false;
        }

        if (!right.getKind().equals("Identifier")) {
            rightType = new Type(right.getKind(),false);
        }
        else if (rightType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Right type is null"));
            return false;
        }

        if (leftType.isArray() && !rightType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Array cannot be used in arithmetic operations: " + leftType.getName() + "[] and " + rightType.getName()));
            return false;
        }
        if (!leftType.isArray() && rightType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Array cannot be used in arithmetic operations: " + leftType.getName() + " and " + rightType.getName() + "[]"));
            return false;
        }
        if (leftType.isArray() && rightType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Array cannot be used in arithmetic operations: " + leftType.getName() + "[] and " + rightType.getName() + "[]"));
            return false;
        }

        if (!node.get("op").equals("&&")) {
            if ((!leftType.getName().equals("int") && !leftType.getName().equals("Integer")) || (!rightType.getName().equals("int") && !rightType.getName().equals("Integer"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Incompatible types in " + node.get("op") + " operation: " + leftType.getName() + " and " + rightType.getName()));
                return false;
            }
        }
        else if ((!leftType.getName().equalsIgnoreCase("boolean") && !rightType.getName().equalsIgnoreCase("boolean"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Incompatible types in " + node.get("op") + " operation: " + leftType.getName() + " and " + rightType.getName()));
                return false;
        }

        return true;
    }

}
