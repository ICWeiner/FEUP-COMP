package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SemanticAnalysisVisitor extends AJmmVisitor<Boolean, Boolean> {
    private final SymbolTable table;
    private final List<Report> reports;

    private String currentMethod;

    public SemanticAnalysisVisitor(SymbolTable table, List<Report> reports) {
        this.table = table;
        this.reports = reports;
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

    private Boolean dealWithConditionalStmt(JmmNode node, Boolean data){
        System.out.println("ConditionalStmt: " + node.getChildren());
        //TODO Arrays
        for (JmmNode child : node.getChildren()) {
            if(child.getKind().equals("BinaryOp") && (!child.get("op").equals("<") || !child.get("op").equals("&&"))){ //TODO é preciso tratar de casos em que, por exemplo, (a+b) < (b+a)??
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Conditional expression not boolean"));
                return false;
            }
        }
        return true;
    }

    private Boolean dealWithArrayAccess(JmmNode node, Boolean data) {
        System.out.println("ArrayAccess: " + node.getChildren());

        JmmNode index = node.getJmmChild(1);
        Type indexType = table.getLocalVariableType(index.get("value"),currentMethod);
        boolean indexIsIdentifier = index.getKind().equals("Identifier");

        if (!indexIsIdentifier) {
            indexType = new Type(index.getKind(),false);
        }
        else if (indexType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Array index is null"));
            return false;
        }
        if(indexType.isArray() || (!indexType.getName().equals("int") && !indexType.getName().equals("Integer"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: "));
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
        boolean leftIsIdentifier = left.getKind().equals("Identifier");
        if (!leftIsIdentifier){
            leftType = new Type(left.getKind(),false);
        }
        else if (leftType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Left type is null"));
            return false;
        }

        boolean rightIsIdentifier = right.getKind().equals("Identifier");
        if (!rightIsIdentifier) {
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

        if (!node.get("op").equals("<") && !node.get("op").equals("&&")) {
            if ((!leftType.getName().equals("int") && !leftType.getName().equals("Integer")) || (!rightType.getName().equals("int") && !rightType.getName().equals("Integer"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Different types: " + leftType.getName() + " and " + rightType.getName()));
                return false;
            }
        }
        else {
            if (!leftType.getName().equals("boolean") || !rightType.getName().equals("boolean")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Different types: " + leftType.getName() + " and " + rightType.getName()));
                return false;
            }
        }

        return true;
    }

    public List<Report> getReports() {
        return reports;
    }
}
