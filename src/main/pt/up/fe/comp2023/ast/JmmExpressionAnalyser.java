package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class JmmExpressionAnalyser extends AJmmVisitor<String, String> {
    private final SymbolTable table;
    private final List<Report> reports;
    private String scope;
    private JmmMethod currentMethod;

    public JmmExpressionAnalyser(SymbolTable table, List<Report> reports) {
        this.table = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {

    }
}
