package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class OllirVisitor extends AJmmVisitor<List<Object>, List<Object>> {
    private final SymbolTable table;
    private JmmMethod currentMethod;
    private final List<Report> reports;
    private String scope;
    private final Set<JmmNode> visited = new HashSet<>();

    public OllirVisitor(SymbolTable table,List<Report> reports){
        this.table = table;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        this.addVisit("Program",this::dealWithProgram);

        this.addVisit("ClassDeclaration",this::dealWithClass);
        this.addVisit("MethodDeclaration", this::dealWithMethodDeclaration);

        // setDefaultVisit(this::defaultVisit);
    }

    private List<Object> defaultVisit(JmmNode node, List<Object> data) {
        return Collections.singletonList("DEFAULT_VISIT");
    }

    private List<Object> dealWithProgram(JmmNode node,List<Object> data){
        for (JmmNode child : node.getChildren()){
            String ollirChild = (String) visit(child, Collections.singletonList("CLASS")).get(0);
        }
        return null;
    }

    private List<Object> dealWithClass(JmmNode node,List<Object> data){
        scope = "CLASS";


        List<String> fields = new ArrayList<>();
        List<String> classBody = new ArrayList<>();

        StringBuilder ollir = new StringBuilder();

        for (String importStmt : this.table.getImports()){
            ollir.append(String.format("Import %s;\n",importStmt));
        }
        ollir.append("\n");

        for(JmmNode child : node.getChildren()){
            String ollirChild = (String) visit(child, Collections.singletonList("CLASS")).get(0);
            System.out.println(ollirChild);
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT")) {
                System.out.println("in cycle");
                System.out.println(child.getKind());
                if (child.getKind().equals("VarDeclaration")) {
                    fields.add(ollirChild);
                } else {
                    classBody.add(ollirChild);
                }
            }
        }

        ollir.append(OllirTemplates.classTemplate(table.getClassName(), table.getSuper()));

        ollir.append(String.join("\n", fields)).append("\n\n");
        ollir.append(OllirTemplates.constructor(table.getClassName())).append("\n\n");
        ollir.append(String.join("\n\n", classBody));

        ollir.append(OllirTemplates.closeBrackets());

        System.out.println("even further in class dealing");
        System.out.println(ollir);

        return Collections.singletonList(ollir.toString());
    }


    private List<Object> dealWithMethodDeclaration(JmmNode node, List<Object> data) {
        if (visited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        visited.add(node);

        scope = "METHOD";

        //List<Type> params = JmmMethod.parseParameters(node.get("params"));



        try {
            if (node.getKind().equals("MainMethod")){
                currentMethod = table.getMethod("main", Collections.singletonList(new Type("String", true)), new Type("void", false));
            }else{
                List<Type> params = JmmMethod.parseParameters(node.get("params"));
                currentMethod = table.getMethod(node.get("name"), params, SymbolTable.getType(node, "return"));
            }


        } catch (Exception e) {
            currentMethod = null;
            e.printStackTrace();
        }

        StringBuilder builder;

        if (node.getKind() == "MainMethod")
            builder = new StringBuilder(OllirTemplates.method(
                    "main",
                    currentMethod.parametersToOllir(),
                    OllirTemplates.type(currentMethod.getReturnType()),
                    true));
        else
            builder = new StringBuilder(OllirTemplates.method(
                    currentMethod.getName(),
                    currentMethod.parametersToOllir(),
                    OllirTemplates.type(currentMethod.getReturnType())));


        List<String> body = new ArrayList<>();

        for (JmmNode child : node.getChildren()) {
            String ollirChild = (String) visit(child, Collections.singletonList("METHOD")).get(0);
            if (ollirChild != null && !ollirChild.equals("DEFAULT_VISIT"))
                if (ollirChild.equals("")) continue;
            body.add(ollirChild);
        }

        builder.append(String.join("\n", body));

        builder.append(OllirTemplates.closeBrackets());

        return Collections.singletonList(builder.toString());
    }
}
