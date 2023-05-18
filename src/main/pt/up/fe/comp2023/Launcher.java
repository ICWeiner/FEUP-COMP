package pt.up.fe.comp2023;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.ast.JmmAnalyser;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;


public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        for (Report report : parserResult.getReports())
            System.out.println(report.toString());

        if (parserResult.getRootNode() != null)
            System.out.println(parserResult.getRootNode().toTree());

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        //Semantic analysis
        JmmAnalyser jmmAnalyser = new JmmAnalyser();

        JmmSemanticsResult semanticResults = jmmAnalyser.semanticAnalysis(parserResult);
        if(!semanticResults.getReports().isEmpty()) return;

        //OLLIR Generation
        JmmOptimizer jmmOptimizer = new JmmOptimizer();

        OllirResult ollirResult=  jmmOptimizer.toOllir(semanticResults);

        System.out.println(ollirResult.getOllirCode());

        //Jasmin Generation
        JasminBackEnd jasminBackEnd = new JasminBackEnd();

        JasminResult jasminResult = jasminBackEnd.toJasmin(ollirResult);


        System.out.println(jasminResult.getJasminCode());

        //jasminResult.run();
        // ... add remaining stages
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        return config;
    }

}
