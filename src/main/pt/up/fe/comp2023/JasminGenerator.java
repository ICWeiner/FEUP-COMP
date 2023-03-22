package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;

import java.util.HashMap;
import java.util.Map;

public class JasminGenerator {
    private ClassUnit classUnit;
    private int CounterStack;
    private int CounterMax;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public String dealWithClass() {
        StringBuilder stringBuilder = new StringBuilder("");

        // class declaration
        stringBuilder.append(".class ").append(classUnit.getClassName()).append("\n");

        // extends declaration
        if (classUnit.getSuperClass() != null) {
            stringBuilder.append(".super ").append(classUnit.getSuperClass()).append("\n");
        } else {
            stringBuilder.append(".super java/lang/Object\n");
        }

        // fields declaration
        for (Field f : classUnit.getFields()) {
            stringBuilder.append(".field '").append(f.getFieldName()).append("' ").append(this.convertType(f.getFieldType())).append("\n");
        }

        for (Method method : classUnit.getMethods()) {
            this.CounterStack = 0;
            this.CounterMax = 0;

            stringBuilder.append(this.dealWithMethodHeader(method));
            String instructions = this.dealtWithMethodIntructions(method);
            if (!method.isConstructMethod()) {
                stringBuilder.append(this.dealWithMethodLimits(method));
                stringBuilder.append(instructions);
            }
        }

        return stringBuilder.toString();
    }

    private String dealWithMethodLimits(Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        int localCount = method.getVarTable().size();
        if (!method.isStaticMethod()) {
            localCount++;
        }
        stringBuilder.append(".limit locals ").append(localCount).append("\n");
        stringBuilder.append(".limit stack ").append(CounterMax).append("\n");
        return stringBuilder.toString();
    }

    private String dealtWithMethodIntructions(Method method) {
        StringBuilder BuilderOfStrings = new StringBuilder();
        method.getVarTable();
        for (Instruction instruction : method.getInstructions()) {
            BuilderOfStrings.append(dealWithInstruction(instruction, method.getVarTable(), method.getLabels()));
            if (instruction instanceof CallInstruction && ((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID) {
                BuilderOfStrings.append("pop\n");
                this.decrementStackCounter(1);
            }
        }
        BuilderOfStrings.append("\n.end method\n");
        return BuilderOfStrings.toString();
    }
}
