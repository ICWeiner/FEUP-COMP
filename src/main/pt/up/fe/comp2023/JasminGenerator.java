package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;

import java.util.HashMap;
import java.util.Map;

import static org.specs.comp.ollir.OperationType.*;

public class JasminGenerator {
    private ClassUnit classUnit;
    private int CounterStack;
    private int CounterMax;
    private int ConditionInt;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public String dealWithClass() {
        StringBuilder BuilderOfStrings = new StringBuilder("");

        // class declaration
        BuilderOfStrings.append(".class public ").append(classUnit.getClassName()).append("\n");

        // extends declaration
        if (classUnit.getSuperClass() != null) {
            BuilderOfStrings.append(".super ").append(classUnit.getSuperClass()).append("\n");
        } else {
            BuilderOfStrings.append(".super java/lang/Object\n");
        }

        // fields declaration
        for (Field f : classUnit.getFields()) {
            BuilderOfStrings.append(".field '").append(f.getFieldName()).append("' ").append(this.convertType(f.getFieldType())).append("\n");
        }

        for (Method method : classUnit.getMethods()) {
            this.CounterStack = 0;
            this.CounterMax = 0;

            BuilderOfStrings.append(this.dealWithMethodHeader(method));
            String instructions = this.dealtWithMethodIntructions(method);
            if (!method.isConstructMethod()) {
                BuilderOfStrings.append(this.dealWithMethodLimits(method));
                BuilderOfStrings.append(instructions);
            }
        }

        return BuilderOfStrings.toString();
    }

    private String dealWithMethodHeader(Method method) {
        if (method.isConstructMethod()) {
            String classSuper = "java/lang/Object";
            if (classUnit.getSuperClass() != null) {
                classSuper = classUnit.getSuperClass();
            }
            return "\n.method public <init>()V\naload_0\ninvokespecial " + classSuper +  ".<init>()V\nreturn\n.end method\n";
        }
        StringBuilder BuilderOfStrings = new StringBuilder("\n.method").append(" ").append(method.getMethodAccessModifier().name().toLowerCase()).append(" ");
        if (method.isStaticMethod()) {
            BuilderOfStrings.append("static ");
        }
        else if (method.isFinalMethod()) {
            BuilderOfStrings.append("final ");
        }
        // Parameters type
        BuilderOfStrings.append(method.getMethodName()).append("(");
        for (Element element: method.getParams()) {
            BuilderOfStrings.append(convertType(element.getType()));
        }
        // Return type
        BuilderOfStrings.append(")").append(this.convertType(method.getReturnType())).append("\n");
        return BuilderOfStrings.toString();
    }

    private String dealWithMethodLimits(Method method) {
        StringBuilder BuilderOfStrings = new StringBuilder();
        int localCount = method.getVarTable().size();
        if (!method.isStaticMethod()) {
            localCount++;
        }
        BuilderOfStrings.append(".limit locals ").append(localCount).append("\n");
        BuilderOfStrings.append(".limit stack ").append(CounterMax).append("\n");
        return BuilderOfStrings.toString();
    }

    private String dealtWithMethodIntructions(Method method) {
        StringBuilder BuilderOfStrings = new StringBuilder();
        method.getVarTable();
        boolean doesntHaveReturnInst =true;
        for (Instruction instruction : method.getInstructions()) {

            if (instruction.getInstType().toString().equals("RETURN")){
                doesntHaveReturnInst =false;
            }

            BuilderOfStrings.append(dealWithInstruction(instruction, method.getVarTable(), method.getLabels()));
            if (instruction instanceof CallInstruction && ((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID) {
                BuilderOfStrings.append("pop\n");
                this.decrementStackCounter(1);
            }
        }
        if(doesntHaveReturnInst){
            BuilderOfStrings.append("return\n");
        }
        BuilderOfStrings.append("\n.end method\n");
        return BuilderOfStrings.toString();
    }

    private String dealWithInstruction(Instruction instruction, HashMap<String, Descriptor> varTable, HashMap<String, Instruction> labels) {
        StringBuilder BuilderOfStrings = new StringBuilder();
        for (Map.Entry<String, Instruction> entry : labels.entrySet()) {
            if (entry.getValue().equals(instruction)) {
                BuilderOfStrings.append(entry.getKey()).append(":\n");
            }
        }
        return switch (instruction.getInstType()) {
            case ASSIGN ->
                    BuilderOfStrings.append(dealWithAssignment((AssignInstruction) instruction, varTable)).toString();
            case NOPER ->
                    BuilderOfStrings.append(dealWithSingleOpInstruction((SingleOpInstruction) instruction, varTable)).toString();
            case BINARYOPER ->
                    BuilderOfStrings.append(dealWithBinaryOpInstruction((BinaryOpInstruction) instruction, varTable)).toString();
            case UNARYOPER -> "Deal with '!' in correct form";
            case CALL ->
                    BuilderOfStrings.append(dealWithCallInstruction((CallInstruction) instruction, varTable)).toString();
            case GOTO ->
                    BuilderOfStrings.append(dealWithGotoInstrutcion((GotoInstruction) instruction, varTable)).toString();
            case PUTFIELD ->
                    BuilderOfStrings.append(dealWithPutFieldInstruction((PutFieldInstruction) instruction, varTable)).toString();
            case GETFIELD ->
                    BuilderOfStrings.append(dealWithGetFieldInstruction((GetFieldInstruction) instruction, varTable)).toString();
            case RETURN ->
                    BuilderOfStrings.append(dealWithReturnInstruction((ReturnInstruction) instruction, varTable)).toString();
            default -> "Error in Instructions";
        };
    }

    private String dealWithReturnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        if(!instruction.hasReturnValue()) return "return";
        String returnString = "";
        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case VOID:
                returnString = "return";
                break;
            case INT32:
            case BOOLEAN:
                returnString = loadElement(instruction.getOperand(), varTable);

                // value →
                this.decrementStackCounter(1);
                returnString += "ireturn";
                break;
            case ARRAYREF:
            case OBJECTREF:
                returnString = loadElement(instruction.getOperand(), varTable);
                // objectref →
                this.decrementStackCounter(1);
                returnString  += "areturn";
                break;
            default:
                break;
        }
        return returnString;
    }

    private String dealWithGetFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String jasminCode = "";
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();
        jasminCode += this.loadElement(obj, varTable); //push object (Class ref) onto the stack
        return jasminCode + "getfield " + classUnit.getClassName() + "/" + var.getName() + " " + convertType(var.getType()) +  "\n";
    }

    private String dealWithPutFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String BuilderOfStrings = "";
        Operand obj = (Operand)instruction.getFirstOperand();
        Operand var = (Operand)instruction.getSecondOperand();
        Element value = instruction.getThirdOperand();
        BuilderOfStrings += this.loadElement(obj, varTable); //push object (Class ref) onto the stack
        BuilderOfStrings += this.loadElement(value, varTable); //store const element on stack
        // ..., objectref, value →
        this.decrementStackCounter(2);
        return BuilderOfStrings + "putfield " + classUnit.getClassName() + "/" + var.getName() + " " + convertType(var.getType()) + "\n";
    }

    private String dealWithGotoInstrutcion(GotoInstruction instruction, HashMap<String, Descriptor> varTable){
        return String.format("goto %s\n", instruction.getLabel());
    }

    private String dealWithCallInstruction(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        String BuilderofStrings = "";
        CallType callType = instruction.getInvocationType();
        switch (callType) {
            case invokespecial ->
                    BuilderofStrings += this.dealWithInvoke(instruction, varTable, callType, ((ClassType) instruction.getFirstArg().getType()).getName());
            case invokestatic ->
                    BuilderofStrings += this.dealWithInvoke(instruction, varTable, callType, ((Operand) instruction.getFirstArg()).getName());
            case invokevirtual ->
                    BuilderofStrings += this.dealWithInvoke(instruction, varTable, callType, ((ClassType) instruction.getFirstArg().getType()).getName());
            case arraylength -> {
                BuilderofStrings += this.loadElement(instruction.getFirstArg(), varTable);
                BuilderofStrings += "arraylength\n";
            }
            case NEW -> BuilderofStrings += this.dealWithNewObject(instruction, varTable);
            default -> {
                return "Error in CallInstruction";
            }
        }
        return BuilderofStrings;
    }

    private String dealWithNewObject(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        Element e = instruction.getFirstArg();
        String BuilderofStrings = "";
        if (e.getType().getTypeOfElement().equals(ElementType.ARRAYREF)) {
            BuilderofStrings += this.loadElement(instruction.getListOfOperands().get(0), varTable);
            BuilderofStrings += "newarray int\n";
        }
        else if (e.getType().getTypeOfElement().equals(ElementType.OBJECTREF)){
            this.incrementStackCounter(2);
            BuilderofStrings += "new " + this.getOjectClassName(((Operand)e).getName()) + "\ndup\n";
        }
        return BuilderofStrings;
    }

    private String dealWithInvoke(CallInstruction instruction, HashMap<String, Descriptor> varTable, CallType callType, String name){
        String BuilderOfStrings = ""; //TODO deal with invokes
        String functionLiteral = ((LiteralElement) instruction.getSecondArg()).getLiteral();
        String parameters = "";
        if (!functionLiteral.equals("\"<init>\"")) {  //does not load element because its a new object, its already done in dealWithNewObject with new and dup
            BuilderOfStrings += this.loadElement(instruction.getFirstArg(), varTable);
        }
        int num_params = 0;
        for (Element element : instruction.getListOfOperands()) {
            BuilderOfStrings += this.loadElement(element, varTable);
            parameters += this.convertType(element.getType());
            num_params++;
        }
        if (!instruction.getInvocationType().equals(CallType.invokestatic)) {
            num_params += 1;
        }
        this.decrementStackCounter(num_params);
        if (instruction.getReturnType().getTypeOfElement() != ElementType.VOID) {
            this.incrementStackCounter(1);
        }
        BuilderOfStrings += callType.name() + " " + this.getOjectClassName(name) + "/" + functionLiteral.replace("\"","") + "(" + parameters + ")" + this.convertType(instruction.getReturnType()) + "\n";
        if (functionLiteral.equals("\"<init>\"") && !name.equals("this")) {
            BuilderOfStrings += this.storeElement((Operand) instruction.getFirstArg(), varTable);
        }
        return BuilderOfStrings;
    }

    private String dealWithAssignment(AssignInstruction instruction, HashMap<String, Descriptor> varTable) {
        String BuilderOfStrings = "";
        Operand operand = (Operand) instruction.getDest();
        if (operand instanceof ArrayOperand) {
            ArrayOperand aoperand = (ArrayOperand) operand;
            // Load array
            BuilderOfStrings += String.format("aload%s\n", this.getVirtualReg(aoperand.getName(), varTable));
            this.incrementStackCounter(1);
            // Load index
            BuilderOfStrings += loadElement(aoperand.getIndexOperands().get(0), varTable);
        }
        BuilderOfStrings += dealWithInstruction(instruction.getRhs(), varTable, new HashMap<String, Instruction>());
        if(!(operand.getType().getTypeOfElement().equals(ElementType.OBJECTREF) && instruction.getRhs() instanceof CallInstruction)) { //if its a new object call does not store yet
            BuilderOfStrings += this.storeElement(operand, varTable);
        }
        return BuilderOfStrings;
    }

    private String dealWithBinaryOpInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        switch (instruction.getOperation().getOpType()) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                return this.dealWithIntOperation(instruction, varTable);
            case LTH:
            case GTE:
            case ANDB:
            case NOTB:
                return this.dealWithBooleanOperation(instruction, varTable);
            default:
                return "Error in BinaryOpInstruction";
        }
    }

    private String dealWithBooleanOperation(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        OperationType ot = instruction.getOperation().getOpType();
        StringBuilder BuilderOfStrings = new StringBuilder();
        switch (instruction.getOperation().getOpType()) {
            case LTH, GTE -> {
                String leftOperand = loadElement(instruction.getLeftOperand(), varTable);
                String rightOperand = loadElement(instruction.getRightOperand(), varTable);
                BuilderOfStrings.append(leftOperand)
                        .append(rightOperand)
                        .append(this.dealWithRelationalOperation(ot, this.getTrueLabel()))
                        .append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");
                // if_icmp decrements 2, iconst increments 1
                this.decrementStackCounter(1);
            }
            case ANDB -> {
                String ifeq = "ifeq " + this.getTrueLabel() + "\n";
                // Compare left operand
                BuilderOfStrings.append(loadElement(instruction.getLeftOperand(), varTable)).append(ifeq);
                this.decrementStackCounter(1);
                // Compare right operand
                BuilderOfStrings.append(loadElement(instruction.getRightOperand(), varTable)).append(ifeq);
                this.decrementStackCounter(1);
                BuilderOfStrings.append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");
                // iconst
                this.incrementStackCounter(1);
            }
            case NOTB -> {
                String operand = loadElement(instruction.getLeftOperand(), varTable);
                BuilderOfStrings.append(operand)
                        .append("ifne ").append(this.getTrueLabel()).append("\n")
                        .append("iconst_1\n")
                        .append("goto ").append(this.getEndIfLabel()).append("\n")
                        .append(this.getTrueLabel()).append(":\n")
                        .append("iconst_0\n")
                        .append(this.getEndIfLabel()).append(":\n");
                // No need to change stack, load increments 1, ifne would dec.1 and iconst would inc.1
            }
            default -> {
                return "Error in BooleansOperations\n";
            }
        }
        this.ConditionInt++;
        return BuilderOfStrings.toString();
    }

    private String dealWithRelationalOperation(OperationType ot, String trueLabel) {
        return switch (ot) {
            case LTH -> String.format("if_icmpge %s\n", trueLabel);
            case GTE -> String.format("if_icmplt %s\n", trueLabel);
            default -> "Error in RelationalOperations\n";
        };
    }

    private String dealWithIntOperation(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        String leftOperand = loadElement(instruction.getLeftOperand(), varTable);
        String rightOperand = loadElement(instruction.getRightOperand(), varTable);
        String operation;
        switch (instruction.getOperation().getOpType()) {
            // ..., value1, value2 →
            // ..., result
            case ADD -> operation = "iadd\n";
            case SUB -> operation = "isub\n";
            case MUL -> operation = "imul\n";
            case DIV -> operation = "idiv\n";
            default -> {
                return "Error in IntOperation\n";
            }
        }
        this.decrementStackCounter(1);
        return leftOperand + rightOperand + operation;
    }


    private String dealWithSingleOpInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return loadElement(instruction.getSingleOperand(), varTable);
    }

    private String loadElement(Element element, HashMap<String, Descriptor> varTable) {
        if (element instanceof LiteralElement) {
            String num = ((LiteralElement) element).getLiteral();
            this.incrementStackCounter(1);
            return this.selectConstType(num) + "\n";
        }
        else if (element instanceof ArrayOperand) {
            ArrayOperand OperandofArray = (ArrayOperand) element;
            // Load array
            String BuilderOfStrings = String.format("aload%s\n", this.getVirtualReg(OperandofArray.getName(), varTable));
            this.incrementStackCounter(1);
            // Load index
            BuilderOfStrings += loadElement(OperandofArray.getIndexOperands().get(0), varTable);
            // ..., arrayref, index →
            // ..., value
            this.decrementStackCounter(1);
            return BuilderOfStrings + "iaload\n";
        }
        else if (element instanceof Operand) {
            Operand OperandofArray = (Operand) element;
            switch (OperandofArray.getType().getTypeOfElement()) {
                case THIS -> {
                    this.incrementStackCounter(1);
                    return "aload_0\n";
                }
                case INT32, BOOLEAN -> {
                    this.incrementStackCounter(1);
                    return String.format("iload%s\n", this.getVirtualReg(OperandofArray.getName(), varTable));
                }
                case OBJECTREF, ARRAYREF -> {
                    this.incrementStackCounter(1);
                    return String.format("aload%s\n", this.getVirtualReg(OperandofArray.getName(), varTable));
                }
                case CLASS -> { //TODO deal with class
                    return "";
                }
                default -> {
                    return "Error in operand loadElements\n";
                }
            }
        }
        return "Error in loadElements\n";
    }

    private String storeElement(Operand operand, HashMap<String, Descriptor> varTable) {
        if (operand instanceof ArrayOperand) {
            // ..., arrayref, index, value →
            this.decrementStackCounter(3);
            return "iastore\n";
        }
        switch (operand.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN: {
                // ..., value →
                this.decrementStackCounter(1);
                return String.format("istore%s\n", this.getVirtualReg(operand.getName(), varTable));
            }
            case OBJECTREF:
            case ARRAYREF: {
                // ..., objectref →
                this.decrementStackCounter(1);
                return String.format("astore%s\n", this.getVirtualReg(operand.getName(), varTable));
            }
            default:
                return "Error in storeElements";
        }
    }

    private String getVirtualReg(String varName, HashMap<String, Descriptor> varTable) {
        int virtualReg = varTable.get(varName).getVirtualReg();
        if (virtualReg > 3) {
            return " " + virtualReg;
        }
        return "_" + virtualReg;
    }

    private String convertType(Type fieldType) {
        ElementType elementType = fieldType.getTypeOfElement();
        String BuilderOfStrings = "";

        if (elementType == ElementType.ARRAYREF) {
            elementType = ((ArrayType) fieldType).getTypeOfElements();
            BuilderOfStrings += "[";
        }

        switch (elementType) {
            case INT32 -> {
                return BuilderOfStrings + "I";
            }
            case BOOLEAN -> {
                return BuilderOfStrings + "Z";
            }
            case STRING -> {
                return BuilderOfStrings + "Ljava/lang/String;";
            }
            case OBJECTREF -> {
                String className = ((ClassType) fieldType).getName();
                return BuilderOfStrings + "L" + this.getOjectClassName(className) + ";";
            }
            case CLASS -> {
                return "CLASS";
            }
            case VOID -> {
                return "V";
            }
            default -> {
                return "Error converting ElementType";
            }
        }
    }

    private String getTrueLabel() {
        return "myTrue" + this.ConditionInt;
    }

    private String getEndIfLabel() {
        return "myEndIf" + this.ConditionInt;
    }

    private String getOjectClassName(String className) {
        for (String _import : classUnit.getImports()) {
            if (_import.endsWith("." + className)) {
                return _import.replaceAll("\\.", "/");
            }
        }
        return className;
    }

    private String selectConstType(String literal){
        return Integer.parseInt(literal) < -1 || Integer.parseInt(literal) > 5 ?
                Integer.parseInt(literal) < -128 || Integer.parseInt(literal) > 127 ?
                        Integer.parseInt(literal) < -32768 || Integer.parseInt(literal) > 32767 ?
                                "ldc " + literal :
                                "sipush " + literal :
                        "bipush " + literal :
                "iconst_" + literal;
    }

    private void decrementStackCounter(int i) {
        this.CounterStack -= i;
    }

    private void incrementStackCounter(int i) {
        this.CounterStack += i;
        if (this.CounterStack > this.CounterMax) {
            this.CounterMax = CounterStack;
        }
    }
}
