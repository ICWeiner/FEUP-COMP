package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class JmmOptimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        JmmNode root = jmmSemanticsResult.getRootNode();

        OllirVisitor ollirVisitor;

        System.out.println("Generating OLLIR...");
        String ollirResult = ollirVisitor.visit();
        System.out.println("OLLIR sucessfully generated");

        return new OllirResult(jmmSemanticsResult, ollirResult,jmmSemanticsResult.getReports());
    }
}
