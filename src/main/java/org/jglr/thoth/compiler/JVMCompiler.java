package org.jglr.thoth.compiler;

import org.jglr.thoth.ThothValue;
import org.jglr.thoth.insns.*;
import org.jglr.thoth.parser.ThothClass;
import org.jglr.thoth.parser.ThothFunc;
import org.objectweb.asm.*;
import thoth.TestClass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class JVMCompiler implements Opcodes {

    private static final Type VALUE_TYPE = Type.getType(ThothValue.class);
    private static final String VALUE_INTERNAL = Type.getInternalName(ThothValue.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type BUILDER_TYPE = Type.getType(StringBuilder.class);
    private final Stack<Integer> stack;
    private String className;

    public JVMCompiler() {
        stack = new Stack<>();
    }

    public void compile(ThothClass clazz) {
        className = "thoth/" + clazz.getName();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        System.out.println("Compiling " + clazz.getName());
        writer.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", new String[0]);
        writer.visitSource(clazz.getName() + ".class", null);
        writer.visitField(ACC_PRIVATE, "_builder", BUILDER_TYPE.getDescriptor(), null, null);
        buildConstructor(clazz, writer);
        for(ThothFunc func : clazz.getFunctions()) {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, func.getName(),
                    Type.getMethodDescriptor(VALUE_TYPE, createArgsParam(func)), null, new String[0]);
            mv.visitCode();
            Label startLabel = new Label();
            mv.visitLabel(startLabel);

            // Empty '_builder' buffer
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "_builder", BUILDER_TYPE.getDescriptor());
            mv.visitInsn(ICONST_0); // loads 0
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "_builder", BUILDER_TYPE.getDescriptor());
            mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "length", Type.getMethodDescriptor(Type.INT_TYPE), false); // calls length
            mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "delete", Type.getMethodDescriptor(BUILDER_TYPE, Type.INT_TYPE, Type.INT_TYPE), false); // calls delete(0, length)

            mv.visitLabel(new Label());
            int paramIndex = 1;
            paramIndex += func.getArgsNumber();
            int resultVar = paramIndex++;

            mv.visitTypeInsn(NEW, VALUE_INTERNAL);
            mv.visitInsn(DUP);
            loadEnum(mv, ThothValue.Types.class, ThothValue.Types.TEXT);
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKESPECIAL, VALUE_INTERNAL, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ThothValue.Types.class), OBJECT_TYPE), false);
            mv.visitVarInsn(ASTORE, resultVar);
            mv.visitLabel(new Label());

            // Actual thoth code
            compileFunction(func, mv);

            // dump buffer content into __result__
            mv.visitVarInsn(ALOAD, resultVar); // get __result__
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "_builder", BUILDER_TYPE.getDescriptor());
            mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "toString", Type.getMethodDescriptor(Type.getType(String.class)), false);
            mv.visitMethodInsn(INVOKEVIRTUAL, VALUE_TYPE.getInternalName(), "setValue", Type.getMethodDescriptor(Type.VOID_TYPE, OBJECT_TYPE), false);

            mv.visitVarInsn(ALOAD, resultVar);
            mv.visitInsn(ARETURN);
            Label endLabel = new Label();
            mv.visitLabel(endLabel);

            paramIndex = 1;
            for(String param : func.getArgumentNames()) {
                mv.visitLocalVariable(param, VALUE_TYPE.getDescriptor(), null, new Label(), new Label(), paramIndex++);
            }
            mv.visitLocalVariable("__result__", VALUE_TYPE.getDescriptor(), null, startLabel, endLabel, resultVar);

            mv.visitMaxs(1, paramIndex);
            mv.visitEnd();
        }
        try {
            File file = new File("./testscompiled/thoth/", "TestClass.class");
            if(!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            if(file.exists())
                file.delete();
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            out.write(writer.toByteArray());
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void compileFunction(ThothFunc func, MethodVisitor mv) {
        Map<String, Label> labelMap = new HashMap<>();
        for(ThothInstruction insn : func.getInstructions()) {
            if(insn instanceof LabelInstruction) {
                String label = ((LabelInstruction) insn).getLabel();
                if(!labelMap.containsKey(label))
                    labelMap.put(label, new Label());
                mv.visitLabel(labelMap.get(label));
            } else if(insn instanceof TextInstruction) {
                String text = ((TextInstruction) insn).getValue();
                addText(text, mv);
            } else if(insn instanceof ParamInstruction) {
                int index = ((ParamInstruction) insn).getIndex();
                stack.push(index);
            } else if(insn instanceof PopInstruction) {
                int index = stack.pop();
                addText(index, mv);
            }
        }
    }

    private void addText(int varIndex, MethodVisitor mv) {
        // this._builder.append(text);
        mv.visitLabel(new Label()); // new label
        mv.visitVarInsn(ALOAD, 0); // get 'this'
        mv.visitFieldInsn(GETFIELD, className, "_builder", BUILDER_TYPE.getDescriptor()); // get '_builder'
        mv.visitVarInsn(ALOAD, varIndex + 1); // add 1 as it starts with 'this' at 0
        mv.visitMethodInsn(INVOKEVIRTUAL, VALUE_TYPE.getInternalName(), "getValue", Type.getMethodDescriptor(Type.getType(Object.class)), false); // call 'getValue'
        mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Object.class), "toString", Type.getMethodDescriptor(Type.getType(String.class)), false); // call 'toString'
        mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "append", Type.getMethodDescriptor(BUILDER_TYPE, Type.getType(String.class)), false); // call 'append(String)'
    }

    private void addText(String text, MethodVisitor mv) {
        // this._builder.append(text);
        mv.visitLabel(new Label()); // new label
        mv.visitVarInsn(ALOAD, 0); // get 'this'
        mv.visitFieldInsn(GETFIELD, className, "_builder", BUILDER_TYPE.getDescriptor()); // get '_builder'
        mv.visitLdcInsn(text); // load text onto the stack
        mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "append", Type.getMethodDescriptor(BUILDER_TYPE, Type.getType(String.class)), false); // call 'append(String)'
    }

    private <T extends Enum> void loadEnum(MethodVisitor mv, Class<T> enumClass, T val) {
        Type enumType = Type.getType(enumClass);
        mv.visitFieldInsn(GETSTATIC, enumType.getInternalName(), val.name(), enumType.getDescriptor());
    }

    private void buildConstructor(ThothClass clazz, ClassWriter writer) {
        MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), null, new String[0]);
        mv.visitCode();
        mv.visitLabel(new Label());
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        mv.visitLabel(new Label());

        mv.visitLabel(new Label());
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, BUILDER_TYPE.getInternalName());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, BUILDER_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        mv.visitFieldInsn(PUTFIELD, className, "_builder", BUILDER_TYPE.getDescriptor());
        mv.visitLabel(new Label());
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private Type[] createArgsParam(ThothFunc func) {
        Type[] types = new Type[func.getArgsNumber()];
        for(int i = 0;i<types.length;i++) {
            types[i] = VALUE_TYPE;
        }
        return types;
    }
}
