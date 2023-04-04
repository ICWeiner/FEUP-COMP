package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

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
}
