package thoth.compiler.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import thoth.compiler.CompilerOptions;
import thoth.compiler.ThothCompileError;
import thoth.compiler.ThothCompilePhase;
import thoth.compiler.ThothType;
import thoth.compiler.bytecode.instructions.*;
import thoth.compiler.resolver.ResolvedClass;
import thoth.compiler.resolver.ResolvedFunction;
import thoth.runtime.*;
import thoth.utils.InstructionHandler;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Compiles previously parsed and resolved Thoth code to bytecode.<br/>
 * This process actually starts by checking the {@link ClassType Thoth class type} and creating a public constructor if needed.<br/>
 * Then it compiles the functions. Finishes by verifying the generated class file to be sure nothing went wrong.<br/>
 * Note: ThothCompiler is stateless and can be multi-threaded
 */
// aka. "Where the magic happens"
public class ThothCompiler extends ThothCompilePhase implements Opcodes {

    private static final Type SET_TYPE = Type.getType(TranslationSet.class);
    private static final Type BUILDER_TYPE = Type.getType(StringBuilder.class);
    private static final Type CACHE_TYPE = Type.getType(HashMap.class);
    private static final Type STRING_TYPE = Type.getType(String.class);
    private static final Type VALUE_TYPE = Type.getType(ThothValue.class);
    private static final Type BOOL_VALUE_TYPE = Type.getType(BoolValue.class);
    private static final Type TEXT_VALUE_TYPE = Type.getType(TextValue.class);
    private static final Type SPACE_VALUE_TYPE = Type.getType(SpaceValue.class);
    private static final Type TRANSLATION_VALUE_TYPE = Type.getType(TranslationValue.class);
    private static final Type TRANSLATION_TYPE = Type.getType(Translation.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private final FunctionParser functionParser;
    private final HashMap<Class<? extends ThothInstruction>, InstructionHandler> insnHandlers;
    private final CompilerOptions options;

    public ThothCompiler() {
        this(CompilerOptions.copyDefault());
    }

    public ThothCompiler(CompilerOptions options) {
        this.options = options;
        functionParser = new FunctionParser();
        insnHandlers = new HashMap<>();

        // Init handlers
        insnHandlers.put(FunctionCallInstruction.class, this::handleFuncCall);
        insnHandlers.put(LoadArgumentInstruction.class, this::handleLdVar);
        insnHandlers.put(LoadBooleanInstruction.class, this::handleLdBool);
        insnHandlers.put(LoadSpaceInstruction.class, this::handleLdSpace);
        insnHandlers.put(LoadTextInstruction.class, this::handleLdText);
        insnHandlers.put(NativeCallInstruction.class, this::handleNativeCall);
    }

    public byte[] compile(ResolvedClass clazz) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        Type classType = Type.getObjectType(clazz.getName().replace('.', '/'));
        String superName = SET_TYPE.getInternalName();
        if(clazz.getClassType() != ClassType.TRANSLATION_SET) {
            superName = OBJECT_TYPE.getInternalName();
        }
        classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL, classType.getInternalName(), null, superName, new String[0]);
        classWriter.visitSource(clazz.getSourceFile(), null);

        String annotDesc = null;
        switch(clazz.getClassType()) {
            case LANGUAGE:
                annotDesc = Type.getDescriptor(LanguageClass.class);
                break;

            case TRANSLATION_SET:
                annotDesc = Type.getDescriptor(TranslationSetClass.class);
                break;

            case UTILIY:
                annotDesc = Type.getDescriptor(UtilityClass.class);
                break;
        }
        AnnotationVisitor visitor = classWriter.visitAnnotation(annotDesc, true);
        visitor.visitEnd();

        if(clazz.getClassType() == ClassType.TRANSLATION_SET) {
            classWriter.visitField(ACC_PRIVATE, "_builder", BUILDER_TYPE.getDescriptor(), null, null);
            classWriter.visitField(ACC_PRIVATE, "_cache", CACHE_TYPE.getDescriptor(), "Ljava/util/HashMap<Ljava/lang/String;Lthoth/runtime/Translation;>;", null);
        }

        buildConstructor(classWriter, classType, clazz);
        if(clazz.getClassType() == ClassType.TRANSLATION_SET) {
            buildInitHandles(classWriter, classType, clazz);
        }

        for(ResolvedFunction function : clazz.getFunctions()) {
            int access = ACC_PUBLIC;
            int localIndex = 0;
            if(clazz.getClassType() != ClassType.TRANSLATION_SET) {
                access |= ACC_STATIC;
            } else {
                localIndex = 1;
            }
            if(options.cachesZeroArgFunctions() && function.getArgumentNames().length == 0) {
                access = ACC_PRIVATE;
                MethodVisitor mv = classWriter.visitMethod(access, "_cacheHelper_"+function.getName(), generateMethodType(function), null, null);
                mv.visitCode();
                for(String n : function.getArgumentNames()) {
                    mv.visitParameter(n, ACC_FINAL);
                    mv.visitLocalVariable(n, VALUE_TYPE.getDescriptor(), null, new Label(), new Label(), localIndex++);
                }
                compileFunction(function, classType, clazz, mv, localIndex);
                mv.visitInsn(ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();

                // Compiles a lookup
                mv = classWriter.visitMethod(access, function.getName(), generateMethodType(function), null, null);
                addTypeAnnotations(function, mv);
                mv.visitCode();
                for(String n : function.getArgumentNames()) {
                    mv.visitParameter(n, ACC_FINAL);
                    mv.visitLocalVariable(n, VALUE_TYPE.getDescriptor(), null, new Label(), new Label(), localIndex++);
                }
                compileCachedFunction(function, classType, clazz, mv, localIndex);
                mv.visitInsn(ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            } else {
                MethodVisitor mv = classWriter.visitMethod(access, function.getName(), generateMethodType(function), null, null);
                addTypeAnnotations(function, mv);
                mv.visitCode();
                for(String n : function.getArgumentNames()) {
                    mv.visitParameter(n, ACC_FINAL);
                    mv.visitLocalVariable(n, VALUE_TYPE.getDescriptor(), null, new Label(), new Label(), localIndex++);
                }
                compileFunction(function, classType, clazz, mv, localIndex);
                mv.visitInsn(ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }
        classWriter.visitEnd();
        byte[] byteArray = classWriter.toByteArray();
        if(/*debug*/true) {
            PrintWriter pw = new PrintWriter(System.out);
            CheckClassAdapter.verify(new ClassReader(byteArray), true, pw);
        }
        return byteArray;
    }

    private void addTypeAnnotations(ResolvedFunction function, MethodVisitor mv) {
        AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Types.class), true);
        AnnotationVisitor arrayVisitor = av.visitArray("value");
        for(ThothType type : function.getTypes()) {
            arrayVisitor.visit(null, type.getName());
        }
        arrayVisitor.visitEnd();
        av.visitEnd();
    }

    private void compileCachedFunction(ResolvedFunction function, Type classType, ResolvedClass clazz, MethodVisitor mv, int localIndex) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, classType.getInternalName(), "_cache", CACHE_TYPE.getDescriptor());
        mv.visitLdcInsn(function.getName());
        mv.visitMethodInsn(INVOKEVIRTUAL, CACHE_TYPE.getInternalName(), "get", Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE), false);
        mv.visitTypeInsn(CHECKCAST, VALUE_TYPE.getInternalName());
        mv.visitInsn(ARETURN);
    }

    private String generateMethodType(ResolvedFunction function) {
        Type[] arguments = new Type[function.getArgumentNames().length];
        for(int i = 0;i<arguments.length;i++) {
            arguments[i] = VALUE_TYPE;
        }
        return Type.getMethodDescriptor(VALUE_TYPE, arguments);
    }

    private int handleLdSpace(ResolvedFunction function, MethodVisitor mv, ThothInstruction insn) {
        mv.visitMethodInsn(INVOKESTATIC, SPACE_VALUE_TYPE.getInternalName(), "getInstance", Type.getMethodDescriptor(SPACE_VALUE_TYPE), false);
        return 1;
    }

    private int handleLdText(ResolvedFunction function, MethodVisitor mv, ThothInstruction insn) {
        String text = ((LoadTextInstruction) insn).getText();
        mv.visitTypeInsn(NEW, TEXT_VALUE_TYPE.getInternalName());
        mv.visitInsn(DUP);
        mv.visitLdcInsn(text);
        mv.visitMethodInsn(INVOKESPECIAL, TEXT_VALUE_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, STRING_TYPE), false);
        return 1;
    }

    private int handleLdBool(ResolvedFunction function, MethodVisitor mv, ThothInstruction insn) {
        LoadBooleanInstruction booleanInstruction = (LoadBooleanInstruction) insn;
        boolean val = booleanInstruction.getValue();
        mv.visitTypeInsn(NEW, BOOL_VALUE_TYPE.getInternalName()); // new BoolValue
        mv.visitInsn(DUP);
        if(val) {
            mv.visitInsn(ICONST_1); // Loads true (1) on the stack
        } else {
            mv.visitInsn(ICONST_0); // Loads false (0) on the stack
        }
        mv.visitMethodInsn(INVOKESPECIAL, BOOL_VALUE_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.BOOLEAN_TYPE), false);
        return 1;
    }

    private int handleLdVar(ResolvedFunction function, MethodVisitor mv, ThothInstruction insn) {
        int offset = function.getOwner().getClassType() == ClassType.TRANSLATION_SET ? 1 : 0;
        int varIndex = ((LoadArgumentInstruction) insn).getIndex()+offset;
        mv.visitVarInsn(ALOAD, varIndex);
        return 1;
    }

    private int handleFuncCall(ResolvedFunction function, MethodVisitor mv, ThothInstruction insn) {
        FunctionCallInstruction callInstruction = (FunctionCallInstruction) insn;
        createFunctionCall(callInstruction.getOwner().getName(), callInstruction.getName(), callInstruction.getArgumentCount(), mv);
        return -callInstruction.getArgumentCount()+1;
    }

    private int handleNativeCall(ResolvedFunction function, MethodVisitor mv, ThothInstruction insn) {
        NativeCallInstruction callInstruction = (NativeCallInstruction)insn;
        int offset = function.getOwner().getClassType() == ClassType.TRANSLATION_SET ? 1 : 0;
        for(int i = 0;i<function.getArgumentNames().length;i++) {
            mv.visitVarInsn(ALOAD, i+offset);
        }
        createFunctionCall(callInstruction.getNativeLocation(), callInstruction.getCaller().getName(), callInstruction.getCaller().getArgumentNames().length, mv);
        return 0;
    }

    private void compileFunction(ResolvedFunction function, Type classType, ResolvedClass clazz, MethodVisitor mv, int localIndex) {
        try {
            int stack = 0;
            CompiledFunction compiled = functionParser.parse(function);
            System.out.println("[====" + compiled.getName() + "====]");
            compiled.getInstructions().forEach(System.out::println);
            mv.visitLabel(new Label());
            for(ThothInstruction insn : compiled.getInstructions()) {
                InstructionHandler handler = getHandler(insn.getClass());
                if(handler != null) {
                    stack += handler.compile(function, mv, insn);
                }
            }
            mv.visitLabel(new Label());
            if(stack > 1) {
                int stackStart = localIndex;
                for (int i = 0; i < stack; i++) {
                    mv.visitVarInsn(ASTORE, localIndex);
                    localIndex++;
                }
                // Compiles result inside StringBuilder
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, classType.getInternalName(), "_builder", BUILDER_TYPE.getDescriptor());
                int index = 0;
                for (int i = 0; i < stack; i++) {
                    mv.visitVarInsn(ALOAD, stackStart + (stack - i - 1));
                    mv.visitMethodInsn(INVOKEVIRTUAL, VALUE_TYPE.getInternalName(), "convertToString", Type.getMethodDescriptor(STRING_TYPE), false);
                    mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "append", Type.getMethodDescriptor(BUILDER_TYPE, STRING_TYPE), false);
                }
                mv.visitInsn(POP);

                // Create TranslationValue holding content
                mv.visitTypeInsn(NEW, TRANSLATION_VALUE_TYPE.getInternalName());
                mv.visitInsn(DUP);

                // Creates Translation instance
                mv.visitTypeInsn(NEW, TRANSLATION_TYPE.getInternalName());
                mv.visitInsn(DUP);
                mv.visitInsn(ICONST_0); // TODO: Use flags/types

                // Transfer builder's content into the Translation instance
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, classType.getInternalName(), "_builder", BUILDER_TYPE.getDescriptor());
                mv.visitMethodInsn(INVOKEVIRTUAL, BUILDER_TYPE.getInternalName(), "toString", Type.getMethodDescriptor(STRING_TYPE), false);

                mv.visitLdcInsn(function.getArgumentNames().length);
                mv.visitTypeInsn(ANEWARRAY, Type.getInternalName(String.class));
                for (int i = 0; i < function.getArgumentNames().length; i++) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(i);
                    mv.visitLdcInsn(function.getArgumentNames()[i]);
                    mv.visitInsn(AASTORE);
                }

                mv.visitMethodInsn(INVOKESPECIAL, TRANSLATION_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, STRING_TYPE, Type.getType(String[].class)), false);

                mv.visitMethodInsn(INVOKESPECIAL, TRANSLATION_VALUE_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, TRANSLATION_TYPE), false);
            }
            mv.visitLabel(new Label());
        } catch (ThothCompileError err) {
            getErrors().add(err);
        }
    }

    private InstructionHandler getHandler(Class<? extends ThothInstruction> insnClass) {
        return insnHandlers.get(insnClass);
    }

    private void createFunctionCall(String owner, String functionName, int argCount, MethodVisitor mv) {
        Type type = Type.getType(owner.replace('.', '/'));
        Type[] argTypes = new Type[argCount];
        for(int i = 0;i<argTypes.length;i++) {
            argTypes[i] = VALUE_TYPE;
        }
        mv.visitMethodInsn(INVOKESTATIC, type.getInternalName(), functionName, Type.getMethodDescriptor(VALUE_TYPE, argTypes), false);
    }

    private void buildInitHandles(ClassWriter classWriter, Type classType, ResolvedClass clazz) {
        MethodVisitor mv = classWriter.visitMethod(ACC_PROTECTED, "initHandles", Type.getMethodDescriptor(Type.VOID_TYPE), null, new String[0]);
        mv.visitCode();
        mv.visitLabel(new Label());
        for(ResolvedFunction f : clazz.getFunctions()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(f.getName());
            mv.visitLdcInsn(f.getArgumentNames().length);
            mv.visitMethodInsn(INVOKEVIRTUAL, SET_TYPE.getInternalName(), "registerHandle", Type.getMethodDescriptor(Type.VOID_TYPE, STRING_TYPE, Type.INT_TYPE), false);
        }
        mv.visitLabel(new Label());
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void buildConstructor(ClassWriter classWriter, Type classType, ResolvedClass clazz) {
        int access = ACC_PRIVATE;
        if(clazz.getClassType() == ClassType.TRANSLATION_SET) {
            access = ACC_PUBLIC;
        }
        MethodVisitor mv = classWriter.visitMethod(access, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), null, new String[0]);
        mv.visitCode();

        mv.visitLabel(new Label());
        if(clazz.getClassType() == ClassType.TRANSLATION_SET) {
            // Call super constructor from TranslationSet
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, SET_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);

            // Loads builder instance
            mv.visitLabel(new Label());
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(NEW, BUILDER_TYPE.getInternalName());
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, BUILDER_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
            mv.visitFieldInsn(PUTFIELD, classType.getInternalName(), "_builder", BUILDER_TYPE.getDescriptor());

            // Loads cache instance
            mv.visitLabel(new Label());
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(NEW, CACHE_TYPE.getInternalName());
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, CACHE_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
            mv.visitFieldInsn(PUTFIELD, classType.getInternalName(), "_cache", CACHE_TYPE.getDescriptor());

            if(options.cachesZeroArgFunctions()) {
                for(ResolvedFunction func : clazz.getFunctions()) {
                    if(func.getArgumentCount() == 0) {
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD, classType.getInternalName(), "_cache", CACHE_TYPE.getDescriptor());
                        mv.visitLdcInsn(func.getName());
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKEVIRTUAL, classType.getInternalName(), "_cacheHelper_"+func.getName(), Type.getMethodDescriptor(VALUE_TYPE), false);
                        mv.visitMethodInsn(INVOKEVIRTUAL, CACHE_TYPE.getInternalName(), "put", Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE, OBJECT_TYPE), false);
                        mv.visitInsn(POP);
                    }
                }
            }

        } else {
            // Call super constructor from Object
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, OBJECT_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        }
        mv.visitInsn(RETURN);
        mv.visitLabel(new Label());

        // TODO: Cache functions with 0 arguments
        mv.visitMaxs(0, 0); // Let ASM compute maxs and frames
        mv.visitEnd();
    }

    public static Class<?> defineClass(String name, byte[] classData) throws InvocationTargetException, IllegalAccessException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if(cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        try {
            Method m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            m.setAccessible(true);
            Class<?> result = (Class<?>) m.invoke(cl, name, classData, 0, classData.length);
            return result;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}
