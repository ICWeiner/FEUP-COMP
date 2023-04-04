package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.ast.OllirVisitor;
import pt.up.fe.comp2023.ast.SymbolTable;

import java.util.Arrays;

public class JmmOptimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        JmmNode root = jmmSemanticsResult.getRootNode();

        OllirVisitor ollirVisitor = new OllirVisitor((SymbolTable) jmmSemanticsResult.getSymbolTable(),jmmSemanticsResult.getReports());

        System.out.println("Generating OLLIR...");
        String ollirResult = (String) ollirVisitor.visit(root, Arrays.asList("DEFAULT_VISIT")).get(0);
        System.out.println("OLLIR sucessfully generated");

        return new OllirResult(jmmSemanticsResult, ollirResult,jmmSemanticsResult.getReports());
    }
}
