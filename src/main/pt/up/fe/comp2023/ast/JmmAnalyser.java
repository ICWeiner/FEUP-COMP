package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JmmAnalyser implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {

        if (TestUtils.getNumReports(jmmParserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(jmmParserResult, null, Collections.singletonList(errorReport));
        }

        if (jmmParserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null");
            return new JmmSemanticsResult(jmmParserResult, null, Collections.singletonList(errorReport));
        }


        JmmNode node = jmmParserResult.getRootNode();
        SymbolTable table = new SymbolTable();
        List<Report> reports = new ArrayList<>();

        System.out.println("Visitor - Filling Symbol Table...");
        SymbolTableVisitor visitor = new SymbolTableVisitor(table, reports);


        visitor.visit(node);
        System.out.println("Symbol Table Filled!");

        System.out.println(table.print());


        return new JmmSemanticsResult(jmmParserResult, table, reports);
    }
}
