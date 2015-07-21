package thoth.compiler;

import org.objectweb.asm.util.CheckClassAdapter;
import thoth.Constants;
import thoth.lang.ThothValue;
import thoth.insns.*;
import thoth.lang.ThothClass;
import thoth.lang.ThothFunc;
import org.objectweb.asm.*;
import thoth.lang.Translation;
import thothtest.TestClass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class JVMCompiler implements Opcodes {

    private static final Type TRANSLATION_TYPE = Type.getType(Translation.class);
    private static final String TRANSLATION_INTERNAL = Type.getInternalName(Translation.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type BUILDER_TYPE = Type.getType(StringBuilder.class);
    private final Stack<Integer> stack;
    private final List<String> jumpedTo;
    private String className;
    private int localsCount;
    private String interClassName;

    public JVMCompiler() {
        stack = new Stack<>();
        jumpedTo = new ArrayList<>();
        TestClass test = new TestClass();
        Translation a = new Translation(Constants.FLAG_FEMININE, "a", new String[0]);
        Translation b = new Translation(Constants.FLAG_FEMININE | Constants.FLAG_PLURAL, "b", new String[0]);
        Translation c = new Translation(Constants.FLAG_FEMININE | Constants.FLAG_PLURAL, "c", new String[0]);
        System.out.println(test.foo1(a, b, c));
    }

    public void compile(ThothClass clazz) {
        className = "thothtest/" + clazz.getName();
        interClassName = Type.getType(className).getInternalName();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        System.out.println("Compiling " + clazz.getName());
        writer.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", new String[0]);
        writer.visitSource("thothtest/"+clazz.getName() + ".class", "halp");
        writer.visitField(ACC_PRIVATE, "_builder", BUILDER_TYPE.getDescriptor(), null, null);
        buildConstructor(clazz, writer);
        for(ThothFunc func : clazz.getFunctions()) {
            int paramIndex = 1;
            paramIndex += func.getArgsNumber();
            int resultVar = paramIndex++;
            localsCount = paramIndex;

            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, func.getName(),
                    Type.getMethodDescriptor(TRANSLATION_TYPE, createArgsParam(func)), null, new String[0]);
            mv.visitCode();
            Label startLabel = new Label();
            mv.visitLabel(startLabel);

            mv.visitLabel(new Label());

            // Empty '_builder' buffer
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, interClassName, "_builder", BUILDER_TYPE.getDescriptor());
            mv.visitInsn(ICONST_0); // loads 0
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, interClassName, "_builder", BUILDER_TYPE.getDescriptor());
            mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "length", Type.getMethodDescriptor(Type.INT_TYPE), false); // calls length
            mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "delete", Type.getMethodDescriptor(BUILDER_TYPE, Type.INT_TYPE, Type.INT_TYPE), false); // calls delete(0, length)

            mv.visitTypeInsn(NEW, TRANSLATION_INTERNAL);
            mv.visitInsn(DUP);
            mv.visitLdcInsn(func.getTranslation().getFlags());
            mv.visitInsn(ACONST_NULL);
            mv.visitLdcInsn(func.getArgsNumber());
            mv.visitTypeInsn(ANEWARRAY, Type.getInternalName(String.class));
            for(int i = 0;i<func.getArgsNumber();i++) {
                mv.visitInsn(DUP);
                mv.visitLdcInsn(i);
                mv.visitLdcInsn(func.getArgumentNames().get(i));
                mv.visitInsn(AASTORE);
            }
            mv.visitMethodInsn(INVOKESPECIAL, TRANSLATION_INTERNAL, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(String.class), Type.getType(String[].class)), false);
            mv.visitVarInsn(ASTORE, resultVar);
            mv.visitLabel(new Label());

            // Actual thoth code
            compileFunction(func, mv);

            mv.visitLabel(new Label());
            // dump buffer content into __result__
            mv.visitVarInsn(ALOAD, resultVar); // get __result__

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, interClassName, "_builder", BUILDER_TYPE.getDescriptor());
            mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "toString", Type.getMethodDescriptor(Type.getType(String.class)), false);
            mv.visitMethodInsn(INVOKEVIRTUAL, TRANSLATION_INTERNAL, "setRaw", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);

            mv.visitLabel(new Label());
            mv.visitVarInsn(ALOAD, resultVar);
            mv.visitInsn(ARETURN);
            Label endLabel = new Label();
            mv.visitLabel(endLabel);

            paramIndex = 1;
            for(String param : func.getArgumentNames()) {
                mv.visitLocalVariable(param, TRANSLATION_TYPE.getDescriptor(), null, new Label(), new Label(), paramIndex++);
            }
            mv.visitLocalVariable("__result__", TRANSLATION_TYPE.getDescriptor(), null, startLabel, endLabel, resultVar);


            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        writer.visitEnd();
        try {
            File file = new File("./testscompiled/thothtest/", "TestClass.class");
            if(!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            if(file.exists())
                file.delete();
            file.createNewFile();
            byte[] byteArray = writer.toByteArray();
            PrintWriter pw = new PrintWriter(System.out);
            CheckClassAdapter.verify(new ClassReader(byteArray), true, pw);
            FileOutputStream out = new FileOutputStream(file);
            out.write(byteArray);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void compileFunction(ThothFunc func, MethodVisitor mv) {
        Map<String, Label> labelMap = new HashMap<>();
        System.out.println("==== "+func.getName()+" ====");
        for(ThothInstruction insn : func.getInstructions()) {
            System.out.println(insn);
            if(insn instanceof LabelInstruction) {
                String label = ((LabelInstruction) insn).getLabel();
                if(!labelMap.containsKey(label))
                    labelMap.put(label, new Label());
                else
                    System.out.println("fetched "+label);
                mv.visitLabel(labelMap.get(label));
            } else if(insn instanceof TextInstruction) {
                String text = ((TextInstruction) insn).getValue();
                if(!text.isEmpty())
                    addText(text, mv);
            } else if(insn instanceof ParamInstruction) {
                int index = ((ParamInstruction) insn).getIndex();
                stack.push(index);
            } else if(insn instanceof PopInstruction) {
                int index = stack.pop();
                addText(index, mv);
            } else if(insn instanceof FlagInstruction) {
                int flags = ((FlagInstruction) insn).getFlags();
                stack.push(-flags);
            } else if(insn instanceof JumpNotTrueInsn) {
                int val = stack.pop();
                if(val < 0) { // use flags in order to know to jump or not to jump
                    int var = stack.pop();
                    String dest = ((JumpNotTrueInsn) insn).getDestination();
                    Label destination = labelMap.getOrDefault(dest, new Label());
                    labelMap.put(dest, destination);
                    System.out.println("jump to " + dest);
                    mv.visitLabel(new Label());
                    mv.visitVarInsn(ALOAD, var + 1);
                    mv.visitMethodInsn(INVOKEVIRTUAL, TRANSLATION_INTERNAL, "getFlags", Type.getMethodDescriptor(Type.INT_TYPE), false);
                    mv.visitLdcInsn(-val);
                    mv.visitInsn(IAND);
                    mv.visitJumpInsn(IFEQ, destination);
                    mv.visitInsn(POP);
                    mv.visitLabel(new Label());
                    jumpedTo.add(dest);
                }
            }
        }
    }

    private Object[] fillWithLocals(int localsCount) {
        Object[] result = new Object[localsCount];
        for(int i = 0;i<localsCount;i++) {
            result[i] = TOP;
        }
        return result;
    }

    private void addText(int varIndex, MethodVisitor mv) {
        // this._builder.append(text);
        mv.visitLabel(new Label()); // new label
        mv.visitVarInsn(ALOAD, 0); // get 'this'
        mv.visitFieldInsn(GETFIELD, className, "_builder", BUILDER_TYPE.getDescriptor()); // get '_builder'
        mv.visitVarInsn(ALOAD, varIndex + 1); // add 1 as it starts with 'this' at 0
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
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, BUILDER_TYPE.getInternalName());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, BUILDER_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        mv.visitFieldInsn(PUTFIELD, className, "_builder", BUILDER_TYPE.getDescriptor());
        mv.visitLabel(new Label());
        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private Type[] createArgsParam(ThothFunc func) {
        Type[] types = new Type[func.getArgsNumber()];
        for(int i = 0;i<types.length;i++) {
            types[i] = TRANSLATION_TYPE;
        }
        return types;
    }
}
