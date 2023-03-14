package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.List;
import java.util.stream.Collectors;

public class SymbolTableVisitor extends AJmmVisitor<String, String> {
    private final SymbolTable table;
    private String scope;
    private final List<Report> reports;

    public SymbolTableVisitor(SymbolTable table, List<Report> reports) {
        //super(SymbolTableVisitor::reduce);
        this.table = table;
        this.reports = reports;

        //buildVisitor();
    }

    @Override
    protected void buildVisitor() {
        this.addVisit("ImportDeclaration", this::dealWithImport);
        this.addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        this.addVisit("MainMethod", this::dealWithMainDeclaration);
        this.addVisit("CustomMethod", this::dealWithMethodDeclaration);
        this.addVisit("VarDeclaration", this::dealWithMethodDeclaration);
        //this.addVisit("Type", this::dealWithParameter);

        this.addVisit("ImportStmt", this::dealWithProgram); //TODO: sort of hacked into working, should probably fix
        //this.addVisit("Type",this::dealWithParameter);


        this.setDefaultVisit(this::defaultVisit);
    }

    private String dealWithProgram(JmmNode node, String space){
        System.out.println("Program visit happening");
        for ( JmmNode child : node.getChildren()){
            System.out.println("Child of type:" + child + " found");
            visit(child);
        }
        return space + "PROGRAM";
    }

    private String dealWithImport(JmmNode node, String space) {
        System.out.println("Import visit happening");
        table.addImport(node.get("name"));
        return space + "IMPORT";
    }

    private String dealWithClassDeclaration(JmmNode node, String space) {
        System.out.println("Class visit happening");
        table.setClassName(node.get("name"));
        try {
            table.setSuper(node.get("superName"));
        } catch (NullPointerException ignored) {

        }

        scope = "CLASS";

        System.out.println("Class node has :" + node.getNumChildren() + " children");
        for ( JmmNode child : node.getChildren()){
            //System.out.println("Child of type:" + child + " found");
            visit(child);
        }


        return space + "CLASS";
    }

    private String dealWithVarDeclaration(JmmNode node, String space) {
        System.out.println("Var visit happening");
        System.out.println("Var node has the following attributes " +node.getAttributes());
        System.out.println("Var node has this many children " +node.getNumChildren());
        System.out.println(node.getJmmChild(0).getAttributes());
        //Symbol field = new Symbol(SymbolTable.getType(node, "name",node.get("isArray")), varName);

        /*if (scope.equals("CLASS")) {
            if (table.fieldExists(field.getName())) {
                this.reports.add(new Report(
                        ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")),
                        "Variable already declared: " + field.getName()));
                return space + "ERROR";
            }else{
                table.addField(field);
            }
        } else {
            if (table.getCurrentMethod().fieldExists(field.getName())) {
                this.reports.add(new Report(
                        ReportType.ERROR, Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")),
                        "Variable already declared: " + field.getName()));
                return space + "ERROR";
            }
            else{
                table.getCurrentMethod().addLocalVariable(field);
            }

        }*/

        return space + "VARDECLARATION";
    }

    private String dealWithMethodDeclaration(JmmNode node, String space) {
        System.out.println("Method visit happening");
        scope = "METHOD";


        //table.addMethod(node.get("name"), SymbolTable.getType(node, "returnType"));

        System.out.println("Method has this many children:" + node.getNumChildren());
        //for(int i = 0; i < node.getNumChildren())
        for ( JmmNode child : node.getChildren()){//TODO: Not working fix in grammar or here, idk
            System.out.println(child.getAttributes());
            System.out.println("parameter of type: " + child.getKind() + /*" has: "+ node.get("paramKind") +*/" name:" + node.get("paramName") + " found");
            //table.getCurrentMethod().addParameter(new Symbol(SymbolTable.getType(node, "kind"), node.get("name")));
            visit(child);
        }

        //node.put("params", "");

        return space + "METHODDECLARATION";
    }

    private String dealWithParameter(JmmNode node,String nodeName ,String space) {
        System.out.println("Parameter visit happening");
        //System.out.println(node.get("ID"));
        /*if (scope.equals("METHOD")) {
            Symbol field = new Symbol(SymbolTable.getType(node, "kind"), node.get("value"));
            table.getCurrentMethod().addParameter(field);


            String paramType = field.getType().getName() + ((field.getType().isArray()) ? " []" : "");
            node.getJmmParent().put("params", node.getJmmParent().get("params") + paramType + ",");
        } else if (scope.equals("MAIN")) {
            Symbol field = new Symbol(new Type("String", true), node.get("value"));
            table.getCurrentMethod().addParameter(field);

            String paramType = field.getType().getName() + ((field.getType().isArray()) ? " []" : "");
            node.getJmmParent().put("params", node.getJmmParent().get("params") + paramType + ",");
        }*/

        return space + "PARAM";
    }

    private String dealWithMainDeclaration(JmmNode node, String space) {
        System.out.println("Main visit happening");
        scope = "MAIN";

        table.addMethod("main", new Type("void", false));

        node.put("params", "");

        return node.toString();
    }

    private String defaultVisit(JmmNode node, String space) {
        System.out.println("Default visit happening");
        String content = space + node.getKind();
        String attrs = node.getAttributes()
                .stream()
                .filter(a -> !a.equals("line"))
                .map(a -> a + "=" + node.get(a))
                .collect(Collectors.joining(", ", "[", "]"));

        content += ((attrs.length() > 2) ? attrs : "");

        return content;
    }

    private static String reduce(String nodeResult, List<String> childrenResults) {
        var content = new StringBuilder();

        content.append(nodeResult).append("\n");

        for (var childResult : childrenResults) {
            var childContent = StringLines.getLines(childResult).stream()
                    .map(line -> " " + line + "\n")
                    .collect(Collectors.joining());

            content.append(childContent);
        }

        return content.toString();
    }


}
