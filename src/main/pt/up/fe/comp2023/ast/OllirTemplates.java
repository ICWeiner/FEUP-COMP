package pt.up.fe.comp2023.ast;

public class OllirTemplates {
    public static String classTemplate(String name, String extended) {
        if (extended == null) return classTemplate(name);

        StringBuilder ollir = new StringBuilder();
        ollir.append(String.format("%s extends %s", name, extended)).append(openBrackets());
        return ollir.toString();
    }

    public static String classTemplate(String name) {
        StringBuilder ollir = new StringBuilder();
        ollir.append(name).append(openBrackets());
        return ollir.toString();
    }

    public static String constructor(String name) {
        StringBuilder ollir = new StringBuilder();
        ollir.append(".construct ").append(name).append("().V").append(openBrackets());
        ollir.append("invokespecial(this, \"<init>\").V;");
        ollir.append(closeBrackets());
        return ollir.toString();
    }

    public static String openBrackets() {
        return " {\n";
    }

    public static String closeBrackets() {
        return "\n}";
    }
}
