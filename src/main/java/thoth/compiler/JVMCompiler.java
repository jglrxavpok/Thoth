package thoth.compiler;

import org.objectweb.asm.util.CheckClassAdapter;
import thoth.Utils;
import thoth.lang.*;
import thoth.insns.*;
import org.objectweb.asm.*;
import thoth.parser.FunctionCallDef;
import thoth.parser.ThothParser;
import thoth.parser.ThothParserException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class JVMCompiler implements Opcodes {

    private static final Type VALUE_TYPE = Type.getType(ThothValue.class);
    private static final String VALUE_INTERNAL = Type.getInternalName(ThothValue.class);
    private static final Type SET_TYPE = Type.getType(TranslationSet.class);
    private static final String SET_INTERNAL = Type.getInternalName(TranslationSet.class);
    private static final Type TRANSLATION_TYPE = Type.getType(Translation.class);
    private static final String TRANSLATION_INTERNAL = Type.getInternalName(Translation.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type BUILDER_TYPE = Type.getType(StringBuilder.class);
    private static final Type CACHE_TYPE = Type.getType(HashMap.class);
    private final Stack<Integer> stack;
    private final Stack<Integer> tmpStack;
    private final List<String> jumpedTo;
    private final ThothParser parser;
    private String className;
    private int localsCount;
    private String interClassName;

    public static String resultName = "result";
    public static String builderName = "resultBuilder";
    public static String cacheName = "cache";

    public JVMCompiler() {
        stack = new Stack<>();
        tmpStack = new Stack<>();
        jumpedTo = new ArrayList<>();
        parser = new ThothParser();
    }

    public byte[] compileClass(ThothClass clazz) {
        return compileClass(clazz, false);
    }

    public byte[] compileClass(ThothClass clazz, boolean debug) {
        className = clazz.getName();
        interClassName = Type.getType(className).getInternalName();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        System.out.println("Compiling " + clazz.getName());
        writer.visit(V1_8, ACC_PUBLIC, className, null, SET_INTERNAL, new String[0]);
        writer.visitSource(clazz.getSourceFile(), null);
        writer.visitField(ACC_PRIVATE, builderName, BUILDER_TYPE.getDescriptor(), null, null);
        writer.visitField(ACC_PRIVATE, cacheName, CACHE_TYPE.getDescriptor(), null, null);
        buildConstructor(clazz, writer);

        // Register handles
        MethodVisitor handleMV = writer.visitMethod(ACC_PROTECTED, "initHandles",
                Type.getMethodDescriptor(Type.VOID_TYPE), null, new String[0]);
        handleMV.visitCode();

        for(ThothFunc func : clazz.getFunctions()) {
            handleMV.visitLabel(new Label());
            handleMV.visitVarInsn(ALOAD, 0); // this
            handleMV.visitLdcInsn(func.getName());
            handleMV.visitLdcInsn(func.getArgsNumber());
            handleMV.visitMethodInsn(INVOKEVIRTUAL, SET_INTERNAL, "registerHandle", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.INT_TYPE), false);
        }

        handleMV.visitInsn(RETURN);

        handleMV.visitMaxs(0,0);
        handleMV.visitEnd();

        for(ThothFunc func : clazz.getFunctions()) {
            stack.clear();
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, func.getName(),
                    Type.getMethodDescriptor(TRANSLATION_TYPE, createArgsParam(func)), null, new String[0]);
            mv.visitCode();
            compileFunction(func, mv);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        writer.visitEnd();

        byte[] byteArray = writer.toByteArray();
        if(debug) {
            PrintWriter pw = new PrintWriter(System.out);
            CheckClassAdapter.verify(new ClassReader(byteArray), true, pw);
        }
        return byteArray;
    }

    private void compileFunction(ThothFunc func, MethodVisitor mv) {
        if(func.getArgsNumber() == 0) {
            compileCachedFunction(func, mv);
        } else {
            compileFunctionFully(func, mv);
        }
    }

    private void compileCachedFunction(ThothFunc func, MethodVisitor mv) {
        mv.visitLabel(new Label());
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, interClassName, cacheName, CACHE_TYPE.getDescriptor());
        mv.visitLdcInsn(func.getName());
        mv.visitMethodInsn(INVOKEVIRTUAL, CACHE_TYPE.getInternalName(), "get", Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE), false);
        mv.visitTypeInsn(CHECKCAST, TRANSLATION_INTERNAL);
        mv.visitInsn(ARETURN);
        mv.visitLabel(new Label());
    }

    private void compileFunctionFully(ThothFunc func, MethodVisitor mv) {
        int paramIndex = 1;
        paramIndex += func.getArgsNumber();
        int resultVar = paramIndex++;
        localsCount = paramIndex;

        Label startLabel = new Label();
        mv.visitLabel(startLabel);

        mv.visitLabel(new Label());

        emptyBuffer(mv);

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
        compileFunctionBody(func, mv);

        mv.visitLabel(new Label());
        // dump buffer content into __result__
        mv.visitVarInsn(ALOAD, resultVar); // get __result__

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, interClassName, builderName, BUILDER_TYPE.getDescriptor());
        mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "toString", Type.getMethodDescriptor(Type.getType(String.class)), false);
        mv.visitMethodInsn(INVOKEVIRTUAL, TRANSLATION_INTERNAL, "setRaw", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);

        mv.visitLabel(new Label());
        mv.visitVarInsn(ALOAD, resultVar);
        mv.visitInsn(ARETURN);
        Label endLabel = new Label();
        mv.visitLabel(endLabel);

        paramIndex = 1;
        for(String param : func.getArgumentNames()) {
            mv.visitLocalVariable(param, VALUE_TYPE.getDescriptor(), null, new Label(), new Label(), paramIndex++);
        }
        mv.visitLocalVariable(resultName, TRANSLATION_TYPE.getDescriptor(), null, startLabel, endLabel, resultVar);

    }

    private void compileFunctionBody(ThothFunc func, MethodVisitor mv) {
        Map<String, Label> labelMap = new HashMap<>();
        for(ThothInstruction insn : func.getInstructions()) {
            if(insn instanceof LabelInstruction) {
                String label = ((LabelInstruction) insn).getLabel();
                if(!labelMap.containsKey(label))
                    labelMap.put(label, new Label());
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
                    mv.visitLabel(new Label());
                    mv.visitVarInsn(ALOAD, var + 1);
                    mv.visitMethodInsn(INVOKEVIRTUAL, VALUE_INTERNAL, "getValue", Type.getMethodDescriptor(OBJECT_TYPE), false);
                    mv.visitTypeInsn(CHECKCAST, TRANSLATION_INTERNAL);
                    mv.visitLdcInsn(-val);
                    mv.visitMethodInsn(INVOKEVIRTUAL, TRANSLATION_INTERNAL, "hasCorrectFlags", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.INT_TYPE), false);
                    mv.visitJumpInsn(IFEQ, destination);
                    mv.visitLabel(new Label());
                    jumpedTo.add(dest);
                } else {
                    int tmpValue = localsCount++;
                    mv.visitLocalVariable("shouldJump_"+tmpValue, Type.BOOLEAN_TYPE.getDescriptor(), null, new Label(), new Label(), tmpValue);
                    mv.visitLabel(new Label());
                    mv.visitInsn(ICONST_0);
                    mv.visitVarInsn(ISTORE, tmpValue);
                    String dest = ((JumpNotTrueInsn) insn).getDestination();
                    Label destination = labelMap.getOrDefault(dest, new Label());
                    labelMap.put(dest, destination);
                    mv.visitLabel(new Label());
                    mv.visitVarInsn(ALOAD, val + 1);
                    mv.visitMethodInsn(INVOKEVIRTUAL, VALUE_INTERNAL, "getType", Type.getMethodDescriptor(Type.getType(ThothValue.Types.class)), false);
                    mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Enum.class), "ordinal", Type.getMethodDescriptor(Type.INT_TYPE), false);

                    Label defaultHandler = new Label();
                    Label destBool = new Label();
                    Label destNull = new Label();
                    Label endSwitch = new Label();

                    mv.visitLookupSwitchInsn(defaultHandler, new int[]{ThothValue.Types.BOOL.ordinal(), ThothValue.Types.NULL.ordinal()}, new Label[]{destBool, destNull});
                    // switch(getType().ordinal())
                    mv.visitLabel(new Label());
                    { // case BOOL
                        mv.visitLabel(destBool);
                        mv.visitVarInsn(ALOAD, val + 1);
                        mv.visitMethodInsn(INVOKEVIRTUAL, VALUE_INTERNAL, "getValue", Type.getMethodDescriptor(OBJECT_TYPE), false);
                        Label notEqualLabel = new Label();
                        Label endIf = new Label();
                        mv.visitFieldInsn(GETSTATIC, Type.getInternalName(Boolean.class), "TRUE", Type.getDescriptor(Boolean.class));
                        mv.visitJumpInsn(IF_ACMPNE, notEqualLabel);
                        mv.visitLabel(new Label());
                        mv.visitInsn(ICONST_1);

                        mv.visitJumpInsn(GOTO, endIf);
                        mv.visitLabel(notEqualLabel);
                        mv.visitInsn(ICONST_0);
                        mv.visitLabel(endIf);
                        mv.visitVarInsn(ISTORE, tmpValue);

                        mv.visitJumpInsn(GOTO, endSwitch);
                    }
                    { // case NULL
                        mv.visitLabel(destNull);
                        mv.visitInsn(ICONST_0);
                        mv.visitVarInsn(ISTORE, tmpValue);
                        mv.visitJumpInsn(GOTO, endSwitch);
                    }
                    { // default
                        Label nullLabel = new Label();
                        Label endIf = new Label();
                        mv.visitLabel(defaultHandler);
                        mv.visitVarInsn(ALOAD, val + 1);
                        mv.visitMethodInsn(INVOKEVIRTUAL, VALUE_INTERNAL, "getValue", Type.getMethodDescriptor(OBJECT_TYPE), false);
                        mv.visitJumpInsn(IFNULL, nullLabel);
                        mv.visitInsn(ICONST_1);
                        mv.visitJumpInsn(GOTO, endIf);
                        mv.visitLabel(nullLabel);
                        mv.visitInsn(ICONST_0);
                        mv.visitLabel(endIf);
                        mv.visitVarInsn(ISTORE, tmpValue);
                        mv.visitLabel(endSwitch);
                    }

                    mv.visitLabel(new Label());

                    mv.visitVarInsn(ILOAD, tmpValue);
                    mv.visitJumpInsn(IFEQ, destination);
                    jumpedTo.add(dest);
                }
            } else if(insn instanceof GotoInsn) {
                mv.visitLabel(new Label());
                String dest = ((GotoInsn) insn).getDestination();
                Label destination = labelMap.getOrDefault(dest, new Label());
                labelMap.put(dest, destination);
                mv.visitJumpInsn(GOTO, destination);
            } else if(insn instanceof LoadThisInsn) {
                int tmpVal = localsCount++;
                tmpStack.push(tmpVal);
                mv.visitLocalVariable("tmpVal"+tmpVal, Type.getDescriptor(String.class), null, new Label(), new Label(), tmpVal);
                mv.visitLabel(new Label());
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, interClassName, builderName, BUILDER_TYPE.getDescriptor());
                mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "toString", Type.getMethodDescriptor(Type.getType(String.class)), false);
                mv.visitVarInsn(ASTORE, tmpVal);
                mv.visitTypeInsn(NEW, VALUE_INTERNAL);
                mv.visitInsn(DUP);
                loadEnum(mv, ThothValue.Types.class, ThothValue.Types.TRANSLATION);
                mv.visitVarInsn(ALOAD, 0);
            } else if(insn instanceof LoadLocalInsn) {
                int var = ((LoadLocalInsn) insn).getVarIndex();
                mv.visitVarInsn(ALOAD, var + 1);
            } else if(insn instanceof LoadConstantInsn) {
                String var = ((LoadConstantInsn) insn).getConstant();
                mv.visitTypeInsn(NEW, VALUE_INTERNAL);
                mv.visitInsn(DUP);
                loadEnum(mv, ThothValue.Types.class, ThothValue.Types.TEXT);
                mv.visitLdcInsn(var);
                mv.visitMethodInsn(INVOKESPECIAL, VALUE_INTERNAL, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ThothValue.Types.class), OBJECT_TYPE), false);
            } else if(insn instanceof FuncCallInsn) {
                FunctionCallDef def = ((FuncCallInsn) insn).getFunc();
                if(def.isDirect()) {
                    Type type = Type.getMethodType(TRANSLATION_TYPE, createArgsParam(def.nArgs));
                    mv.visitMethodInsn(INVOKEVIRTUAL, interClassName, def.name, type.getDescriptor(), false);
                } else {
                    // fetch tr_name
                    // create array
                    int arrayLocal = localsCount++;
                    mv.visitLocalVariable("arrayLocal" + arrayLocal, Type.getDescriptor(ThothValue[].class), null, new Label(), new Label(), arrayLocal);
                    mv.visitLdcInsn(def.nArgs);
                    mv.visitTypeInsn(ANEWARRAY, Type.getInternalName(ThothValue.class));
                    for(int i = 0;i<def.nArgs;i++) {
                        int arrayLocal1 = localsCount++;
                        mv.visitLocalVariable("arrayElemLocal" + arrayLocal, Type.getDescriptor(ThothValue.class), null, new Label(), new Label(), arrayLocal1);
                        mv.visitVarInsn(ASTORE, arrayLocal1);
                        mv.visitInsn(DUP);
                        mv.visitLdcInsn(i);
                        mv.visitVarInsn(ALOAD, arrayLocal1);
                        mv.visitInsn(AASTORE);
                    }
                    mv.visitVarInsn(ASTORE, arrayLocal);
                    mv.visitMethodInsn(INVOKEVIRTUAL, VALUE_INTERNAL, "getValue", Type.getMethodDescriptor(OBJECT_TYPE), false);
                    mv.visitMethodInsn(INVOKEVIRTUAL, OBJECT_TYPE.getInternalName(), "toString", Type.getMethodDescriptor(Type.getType(String.class)), false);
                    mv.visitVarInsn(ALOAD, arrayLocal);

                    Type type = Type.getMethodType(TRANSLATION_TYPE, Type.getType(String.class), Type.getType(ThothValue[].class));
                    mv.visitMethodInsn(INVOKEVIRTUAL, SET_INTERNAL, "getTranslation", type.getDescriptor(), false);
                }
                mv.visitMethodInsn(INVOKESPECIAL, VALUE_INTERNAL, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ThothValue.Types.class), OBJECT_TYPE), false);
            } else if(insn instanceof PopAfterFuncInsn) {
                emptyBuffer(mv);
                int local = localsCount++;
                mv.visitVarInsn(ASTORE, local);
                int tmpVal = tmpStack.pop();
                addRawText(tmpVal, mv);
                mv.visitLocalVariable("localFuncRes" + local, VALUE_TYPE.getDescriptor(), null, new Label(), new Label(), local);
                addText(local-1, mv);
            }
        }

    }

    private void emptyBuffer(MethodVisitor mv) {
        // Empty '_builder' buffer
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, interClassName, builderName, BUILDER_TYPE.getDescriptor());
        mv.visitInsn(ICONST_0); // loads 0
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, interClassName, builderName, BUILDER_TYPE.getDescriptor());
        mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "length", Type.getMethodDescriptor(Type.INT_TYPE), false); // calls length
        mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "delete", Type.getMethodDescriptor(BUILDER_TYPE, Type.INT_TYPE, Type.INT_TYPE), false); // calls delete(0, length)
        mv.visitInsn(POP);

    }

    private void addRawText(int varIndex, MethodVisitor mv) {
        // this._builder.append(text);
        mv.visitLabel(new Label()); // new label
        mv.visitVarInsn(ALOAD, 0); // get 'this'
        mv.visitFieldInsn(GETFIELD, className, builderName, BUILDER_TYPE.getDescriptor()); // get '_builder'
        mv.visitVarInsn(ALOAD, varIndex); // add 1 as it starts with 'this' at 0
        mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "append", Type.getMethodDescriptor(BUILDER_TYPE, Type.getType(String.class)), false); // call 'append(String)'
        mv.visitInsn(POP);
    }

    private void addText(int varIndex, MethodVisitor mv) {
        // this._builder.append(text);
        mv.visitLabel(new Label()); // new label
        mv.visitVarInsn(ALOAD, 0); // get 'this'
        mv.visitFieldInsn(GETFIELD, className, builderName, BUILDER_TYPE.getDescriptor()); // get '_builder'
        mv.visitVarInsn(ALOAD, varIndex + 1); // add 1 as it starts with 'this' at 0
        mv.visitMethodInsn(INVOKEVIRTUAL, VALUE_INTERNAL, "getValue", Type.getMethodDescriptor(OBJECT_TYPE), false); // call 'getValue'
        mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Object.class), "toString", Type.getMethodDescriptor(Type.getType(String.class)), false); // call 'toString'
        mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "append", Type.getMethodDescriptor(BUILDER_TYPE, Type.getType(String.class)), false); // call 'append(String)'
        mv.visitInsn(POP);
    }

    private void addText(String text, MethodVisitor mv) {
        // this._builder.append(text);
        mv.visitLabel(new Label()); // new label
        mv.visitVarInsn(ALOAD, 0); // get 'this'
        mv.visitFieldInsn(GETFIELD, className, builderName, BUILDER_TYPE.getDescriptor()); // get '_builder'
        mv.visitLdcInsn(text); // load text onto the stack
        mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "append", Type.getMethodDescriptor(BUILDER_TYPE, Type.getType(String.class)), false); // call 'append(String)'
        mv.visitInsn(POP);
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
        mv.visitMethodInsn(INVOKESPECIAL, SET_INTERNAL, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);

        mv.visitLabel(new Label());
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, BUILDER_TYPE.getInternalName());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, BUILDER_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        mv.visitFieldInsn(PUTFIELD, className, builderName, BUILDER_TYPE.getDescriptor());
        mv.visitLabel(new Label());

        // Cache functions with 0 argument
        // Create a HashMap<String, Translation> instance and add wanted functions
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, CACHE_TYPE.getInternalName());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, CACHE_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        mv.visitFieldInsn(PUTFIELD, className, cacheName, CACHE_TYPE.getDescriptor());
        clazz.getFunctions().forEach(f -> {
            if (f.getArgsNumber() == 0) {
                // Get the cache instance
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, cacheName, CACHE_TYPE.getDescriptor());

                // Loads function name
                mv.visitLdcInsn(f.getName());

                // Creates translation instance
                mv.visitTypeInsn(NEW, TRANSLATION_INTERNAL);
                mv.visitInsn(DUP);
                mv.visitLdcInsn(f.getTranslation().getFlags());
                mv.visitLdcInsn(f.getTranslation().getRaw());
                mv.visitInsn(ICONST_0);
                mv.visitTypeInsn(ANEWARRAY, Type.getInternalName(String.class));
                mv.visitMethodInsn(INVOKESPECIAL, TRANSLATION_INTERNAL, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(String.class), Type.getType(String[].class)), false);

                // Cache the translation
                mv.visitMethodInsn(INVOKEVIRTUAL, CACHE_TYPE.getInternalName(), "put", Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE, OBJECT_TYPE), false);
                mv.visitInsn(POP);
            }
        });
        mv.visitLabel(new Label());

        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private Type[] createArgsParam(ThothFunc func) {
        return createArgsParam(func.getArgsNumber());
    }

    private Type[] createArgsParam(int nArgs) {
        Type[] types = new Type[nArgs];
        for(int i = 0;i<types.length;i++) {
            types[i] = VALUE_TYPE;
        }
        return types;
    }

    public byte[] compile(URL fileLocation) throws IOException, ThothParserException {
        return compile(fileLocation, false);
    }

    public byte[] compile(URL fileLocation, boolean debug) throws IOException, ThothParserException {
        InputStream input = fileLocation.openStream();
        String path = fileLocation.getPath();
        String file = path.substring(path.lastIndexOf('/') + 1);
        return compile(input, file, debug);
    }

    public byte[] compile(InputStream fileLocation, String file) throws IOException, ThothParserException {
        return compile(fileLocation, file, false);
    }

    public byte[] compile(InputStream fileLocation, String file, boolean debug) throws IOException, ThothParserException {
        String code = Utils.readString(fileLocation, "UTF-8");
        return compile(code, file, debug);
    }

    public byte[] compile(String rawCode, String file) throws ThothParserException {
        return compile(rawCode, file, false);
    }

    public byte[] compile(String rawCode, String file, boolean debug) throws ThothParserException {
        return compileClass(parser.parseRaw(rawCode, file), debug);
    }

    public Class<? extends TranslationSet> defineClass(String name, byte[] classData) throws InvocationTargetException, IllegalAccessException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if(cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        try {
            Method m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            m.setAccessible(true);
            Class<?> result = (Class<?>) m.invoke(cl, name, classData, 0, classData.length);
            if(TranslationSet.class.isAssignableFrom(result)) {
                return (Class<? extends TranslationSet>)result;
            } else {
                throw new IllegalArgumentException("JVMCompiler does not allow you to define non-Thoth classes");
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}
