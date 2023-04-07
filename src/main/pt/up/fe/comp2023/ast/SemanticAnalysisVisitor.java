package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SemanticAnalysisVisitor extends AJmmVisitor<Boolean, Boolean> {
    private final SymbolTable table;
    private final List<Report> reports;

    private String currentMethod;

    public SemanticAnalysisVisitor(SymbolTable table, List<Report> reports) {
        this.table = table;
        this.reports = reports; //TODO: os reports estão a aparecer duplicados
    }

    @Override
    protected void buildVisitor() {
        this.setDefaultVisit(this::dealWithDefault);
        this.addVisit("Program", this::dealWithProgram);
        this.addVisit("MainMethod", this::dealWithMainMethod);
        this.addVisit("CustomMethod", this::dealWithCustomMethod);
        this.addVisit("BinaryOp", this::dealWithBinaryOp);
        this.addVisit("IfElseStmt", this::dealWithConditionalStmt);
        this.addVisit("WhileStmt", this::dealWithConditionalStmt);
        this.addVisit("ArrayAccess", this::dealWithArrayAccess);
        this.addVisit("Assignment", this::dealWithAssignment);
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

    private Boolean dealWithMainMethod(JmmNode node, Boolean data){
        System.out.println("Main Method: " + node);
        currentMethod = "main";
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return true;
    }

    private Boolean dealWithCustomMethod(JmmNode node, Boolean data){
        System.out.println("Custom Method: " + node);
        List<String> methods = table.getMethods();
        for (String method : methods) {
            if(node.getJmmChild(0).get("name").equals(method)) {
                currentMethod = method;
                break;
            }
        }
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return true;
    }

    private Boolean dealWithAssignment(JmmNode node, Boolean data){
        System.out.println("Assignment: " + node + " " + node.getChildren());

        Type nodeType = table.getLocalVariableType(node.get("name"),currentMethod);
        if(nodeType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Assignment variable type is null"));
            return false;
        }
        //TODO tratar de imports
        JmmNode child = node.getChildren().get(0);
        //System.out.println("bbbbb " + nodeType.getName() + " " + child.getKind());
        if(!child.getKind().equals("Identifier")) {
            if (!(nodeType.isArray() && nodeType.getName().equals("int") && child.getKind().equals("IntArrayDeclaration"))
                    && !(!nodeType.isArray() && nodeType.getName().equals("int") && child.getKind().equals("Integer"))
                    && !(nodeType.getName().equals("boolean") && child.getKind().equals("Boolean"))
                    && !(child.getKind().equals("GeneralDeclaration") && nodeType.getName().equals(child.get("name")))
                    && !(child.getKind().equals("GeneralDeclaration") && nodeType.getName().equals(child.get("name")))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Assign " + nodeType.getName() + " to " + child.getKind()));
                return false;
            }
        }
        else {
            Type childType = table.getLocalVariableType(child.get("value"),currentMethod);
            if(childType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Assign is null"));
                return false;
            }

            System.out.println("imports: " + table.getImports() + " childType: " + childType.getName() + " nodeType: " + nodeType.getName());

            for (String test: table.getImports()) {
                System.out.println(test);
                System.out.println(test.contains(nodeType.getName()));
                System.out.println(test.contains(childType.getName()));
            }
            System.out.println("childType: " + childType.getName());
            System.out.println("contains childType: " + (table.getImports().contains(childType.getName())) + " contains node: " + table.getImports().contains(nodeType.getName()));
            if(!childType.getName().equals(nodeType.getName()) && !table.getImports().contains(childType.getName()) && !table.getImports().contains(nodeType.getName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Class not imported, assign " + nodeType.getName() + " to " + childType.getName()));
                return false;
            }
        }

        return true;
    }

    private Boolean dealWithConditionalStmt(JmmNode node, Boolean data){
        System.out.println("ConditionalStmt: " + node.getChildren());

        JmmNode child = node.getChildren().get(0);

        if (child.getKind().equals("Identifier")) {
            Type childType = table.getLocalVariableType(child.get("value"),currentMethod);
            if (childType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Conditional statement is null"));
                return false;
            }
            if (childType.isArray()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Conditional statement is an array"));
                return false;
            }
            if (!childType.getName().equals("Boolean")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Conditional statement is not boolean"));
                return false;
            }
        }
        else if(child.getKind().equals("BinaryOp") && (child.get("op").equals("<") || child.get("op").equals("&&"))) {
            JmmNode left = node.getChildren().get(0).getChildren().get(0);
            JmmNode right = node.getChildren().get(0).getChildren().get(1);

            Type leftType = table.getLocalVariableType(left.get("value"),currentMethod);
            Type rightType = table.getLocalVariableType(right.get("value"),currentMethod);

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
            Type arrayType = table.getLocalVariableType(array.get("value"),currentMethod);
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
        Type indexType = table.getLocalVariableType(index.get("value"),currentMethod);

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
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        Type leftType = table.getLocalVariableType(left.get("value"),currentMethod);
        Type rightType = table.getLocalVariableType(right.get("value"),currentMethod);

        //TODO é preciso ver se uma variavel foi inicializada duas vezes??
        System.out.println("plusObject:");
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
        else {
            if ((!leftType.getName().equals("boolean") && !rightType.getName().equals("boolean")) || (!leftType.getName().equals("Boolean") && !rightType.getName().equals("Boolean"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Incompatible types in " + node.get("op") + " operation: " + leftType.getName() + " and " + rightType.getName()));
                return false;
            }
        }

        return true;
    }

    public List<Report> getReports() {
        return reports;
    }
}
