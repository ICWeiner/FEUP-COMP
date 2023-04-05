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
        this.addVisit("CustomMethod", this::dealWithCustomMethod);
        this.addVisit("BinaryOp", this::dealWithBinaryOp);
        //this.addVisit("Assignment", this::dealWithAssignment);
    }

    private Boolean dealWithDefault(JmmNode node, Boolean data) {
        System.out.println("Default: " + node);
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return null;
    }

    private Boolean dealWithProgram(JmmNode node, Boolean data){
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return null;
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
        return null;
    }

    private Boolean dealWithBinaryOp(JmmNode node, Boolean data) {
        System.out.println("BinaryOp: " + node.getJmmChild(0) + " " + node.getJmmChild(1));

        if (node.getNumChildren() != 2) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Wrong number of operands"));
            return false;
        }
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);

        List<Symbol> localVariables = table.getLocalVariables(currentMethod);
        //System.out.println("aaaaaaaa " + localVariables);
        Type leftType = null;
        Type rightType = null;
        for(Symbol localVariable : localVariables) { //TODO verificar quando não é identifier (ou seja, qd é só um int)
            if(localVariable.getName().equals(left.get("value"))) { //é preciso ver se uma variavel foi inicializada duas vezes??
                leftType = localVariable.getType();
            }
            if(localVariable.getName().equals(right.get("value"))) {
                rightType = localVariable.getType();
            }
        }

        if(leftType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Left type is null"));
            return false;
        }
        if(rightType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Right type is null"));
            return false;
        }
        if(!leftType.getName().equals(rightType.getName())) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Different types: " + leftType.getName() + " and " + rightType.getName()));
            return false;
        }
        if(leftType.isArray() && !rightType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Different types: " + leftType.getName() + "[] and " + rightType.getName()));
            return false;
        }
        if(!leftType.isArray() && rightType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Error: Different types: " + leftType.getName() + " and " + rightType.getName() + "[]"));
            return false;
        }

        return true;
    }

    public List<Report> getReports() {
        return reports;
    }
}
