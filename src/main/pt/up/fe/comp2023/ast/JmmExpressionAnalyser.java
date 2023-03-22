package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;
import java.util.Map;

public class JmmExpressionAnalyser extends AJmmVisitor<Boolean, Map.Entry<String, String>> {
    private final SymbolTable table;
    private final List<Report> reports;
    //private String scope;
    private JmmMethod currentMethod;

    public JmmExpressionAnalyser(SymbolTable table, List<Report> reports) {
        this.table = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        this.setDefaultVisit(this::dealWithDefault);
        this.addVisit("Program", this::dealWithProgram);
        //this.addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        this.addVisit("ArrayAccess", this::dealWithArrayAccess); //TODO verify later
    }

    private Map.Entry<String, String> dealWithDefault(JmmNode node, Boolean data) {
        System.out.println("Estou no default");
        return null;
    }

    private Map.Entry<String, String> dealWithProgram(JmmNode node, Boolean data){
        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        return null;
    }
    private Map.Entry<String, String> dealWithArrayAccess(JmmNode node, Boolean data) {
        JmmNode index = node.getChildren().get(0);
        Map.Entry<String, String> indexReturn = visit(index, true);

        if (!indexReturn.getKey().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(index.get("line")), Integer.parseInt(index.get("col")), "Array access index is not an expression of type integer: " + index));
            return Map.entry("error", "null");
        }

        return Map.entry("index", indexReturn.getValue());
    }
}
