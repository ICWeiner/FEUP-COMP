package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolTableVisitor extends AJmmVisitor<String, String> {
    private final SymbolTable table;
    private String scope;
    private final List<Report> reports;

    public SymbolTableVisitor(SymbolTable table, List<Report> reports) {
        this.table = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        this.addVisit("ImportDeclaration", this::dealWithImport);
        this.addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        this.addVisit("MethodDeclaration", this::dealWithMethodDeclaration);;
        this.addVisit("ImportStmt", this::dealWithProgram); //TODO: sort of hacked into working, should probably fix
    }

    private String dealWithProgram(JmmNode node, String space){
        for ( JmmNode child : node.getChildren()){
            visit(child);
        }
        return space + "PROGRAM";
    }

    private String dealWithImport(JmmNode node, String space) {
        table.addImport(node.get("name"));
        return space + "IMPORT";
    }

    private String dealWithClassDeclaration(JmmNode node, String space) {
        table.setClassName(node.get("name"));
        try {
            table.setSuper(node.get("superName"));
        } catch (NullPointerException ignored) {

        }

        scope = "CLASS";

        for ( JmmNode child : node.getChildren()){
            if(child.getKind().equals("VarDeclaration")){
                table.addField(new Symbol(new Type(child.getJmmChild(0).get("typeName"), (Boolean) child.getJmmChild(0).getObject("isArray")),child.getJmmChild(0).get("name")));
            }else{
                visit(child);
            }
        }

        return space + "CLASS";
    }


    private String dealWithMethodDeclaration(JmmNode node, String space) {
        scope = "METHOD";

        if (node.getKind().equals("MainMethod")){
            scope = "MAIN";
            table.addMethod("main", new Type("void", false));
            node.put("params", "");
        } else{
            for(JmmNode child: node.getChildren()){
                if(child.getIndexOfSelf() == 0){
                    table.addMethod(child.get("name"),new Type(child.get("typeName"), (Boolean) child.getObject("isArray")));
                }
                else if(child.getKind().equals("varDeclaration")){
                    table.addField(new Symbol(new Type(child.get("typeName"), (Boolean) child.getObject("isArray")),child.get("name")));
                    //addLocalVariable(declaration, localVars); add local var here
                }
                else if(child.getKind().equals("Type")) {
                    table.getCurrentMethod().addParameter(new Symbol(new Type(child.get("typeName"), (Boolean) child.getObject("isArray")),child.get("name")));
                }
            }
        }


        return space + "METHODDECLARATION";
    }
}