package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;


import java.util.ArrayList;
import java.util.List;


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
        this.addVisit("Program", this::dealWithProgram);
    }

    private String dealWithProgram(JmmNode node, String space){
        for ( JmmNode child : node.getChildren()){
            visit(child);
        }
        return space + "PROGRAM";
    }

    private String dealWithImport(JmmNode node, String space) {
        StringBuilder importName = new StringBuilder();
        for(String name: (ArrayList<String>) node.getObject("name")){
            importName.append(name);
            importName.append(".");
        }
        importName.setLength(importName.length() - 1);//remove last character
        table.addImport(String.valueOf(importName));
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

        if (node.getKind().equals("MainMethod")){//TODO: is this really needed, cant we just treat main like any other method? :thinking:
            scope = "MAIN";
            table.addMethod("main", new Type("void", false));
            //node.put("params", "");
            for(JmmNode child: node.getChildren()){
                populateMethod(child);
            }
        } else{
            for(JmmNode child: node.getChildren()){
                if(child.getIndexOfSelf() == 0){
                    table.addMethod(child.get("name"),new Type(child.get("typeName"), (Boolean) child.getObject("isArray")));
                }
                else populateMethod(child);
            }
        }

        return space + "METHODDECLARATION";
    }

    private void populateMethod(JmmNode child) {//Add fields and local vars to corresponding method in symbol table
        if(child.getKind().equals("VarDeclaration")){
            table.addFieldToCurrentMethod(new Symbol(new Type(child.getJmmChild(0).get("typeName"), (Boolean) child.getJmmChild(0).getObject("isArray")),child.getJmmChild(0).get("name")));
        }
        else if(child.getKind().equals("Type")) {
            table.addParameterToCurrentMethod(new Symbol(new Type(child.get("typeName"), (Boolean) child.getObject("isArray")),child.get("name")));
        }
    }
}