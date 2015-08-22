package thoth.compiler.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import thoth.compiler.ThothCompilePhase;
import thoth.compiler.bytecode.instructions.*;
import thoth.compiler.resolver.ResolvedClass;
import thoth.compiler.resolver.ResolvedFunction;
import thoth.runtime.TextValue;
import thoth.runtime.ThothValue;
import thoth.runtime.Translation;
import thoth.runtime.TranslationSet;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * ThothCompiler is stateless and can be multi-threaded
 */
public class ThothCompiler extends ThothCompilePhase implements Opcodes {

    private static final Type SET_TYPE = Type.getType(TranslationSet.class);
    private static final Type BUILDER_TYPE = Type.getType(StringBuilder.class);
    private static final Type CACHE_TYPE = Type.getType(HashMap.class);
    private static final Type STRING_TYPE = Type.getType(String.class);
    private static final Type VALUE_TYPE = Type.getType(ThothValue.class);
    private static final Type TEXT_VALUE_TYPE = Type.getType(TextValue.class);
    private static final Type TRANSLATION_TYPE = Type.getType(Translation.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private final FunctionParser functionParser;

    public ThothCompiler() {
        functionParser = new FunctionParser();
    }

    public byte[] compile(ResolvedClass clazz) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        Type classType = Type.getObjectType(clazz.getName().replace('.', '/'));
        String superName = SET_TYPE.getInternalName();
        if(!clazz.isTranslationSet()) {
            superName = OBJECT_TYPE.getInternalName();
        }
        classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL, classType.getInternalName(), null, superName, new String[0]);
        classWriter.visitSource(clazz.getSourceFile(), null);

        if(clazz.isTranslationSet()) {
            classWriter.visitField(ACC_PRIVATE, "_builder", BUILDER_TYPE.getDescriptor(), null, null);
            classWriter.visitField(ACC_PRIVATE, "_cache", CACHE_TYPE.getDescriptor(), null, null);
        }

        buildConstructor(classWriter, classType, clazz);
        if(clazz.isTranslationSet()) {
            buildInitHandles(classWriter, classType, clazz);
        }

        for(ResolvedFunction function : clazz.getFunctions()) {
            MethodVisitor mv = classWriter.visitMethod(ACC_PUBLIC, function.getName(), generateMethodType(function), null, null);
            mv.visitCode();
            int localIndex = 1;
            for(String n : function.getArgumentNames()) {
                mv.visitParameter(n, ACC_FINAL);
                mv.visitLocalVariable(n, VALUE_TYPE.getDescriptor(), null, new Label(), new Label(), localIndex++);
            }
            compileFunction(function, mv);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        classWriter.visitEnd();
        byte[] byteArray = classWriter.toByteArray();
        if(/*debug*/true) {
            PrintWriter pw = new PrintWriter(System.out);
            CheckClassAdapter.verify(new ClassReader(byteArray), true, pw);
        }
        return byteArray;
    }

    private String generateMethodType(ResolvedFunction function) {
        Type[] arguments = new Type[function.getArgumentNames().length];
        for(int i = 0;i<arguments.length;i++) {
            arguments[i] = VALUE_TYPE;
        }
        return Type.getMethodDescriptor(VALUE_TYPE, arguments);
    }

    private void compileFunction(ResolvedFunction function, MethodVisitor mv) {
        CompiledFunction compiled = functionParser.parse(function);
        System.out.println("[===="+compiled.getName()+"====]");
        compiled.getInstructions().forEach(System.out::println);
        mv.visitLabel(new Label());
        for(ThothInstruction insn : compiled.getInstructions()) {
            if(insn instanceof LoadTextInstruction) {
                String text = ((LoadTextInstruction) insn).getText();
                mv.visitTypeInsn(NEW, TEXT_VALUE_TYPE.getInternalName());
                mv.visitInsn(DUP);
                mv.visitLdcInsn(text);
                mv.visitMethodInsn(INVOKESPECIAL, VALUE_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, STRING_TYPE), false);
            } else if(insn instanceof LoadArgumentInstruction) {
                int varIndex = ((LoadArgumentInstruction) insn).getIndex()+1;
                mv.visitVarInsn(ALOAD, varIndex);
            } else if(insn instanceof FunctionCallInstruction) {
                FunctionCallInstruction callInstruction = (FunctionCallInstruction) insn;
                handleFunctionCall(callInstruction.getOwner().getName(), callInstruction.getName(), callInstruction.getArgumentCount(), mv);
            } else if(insn instanceof NativeCallInstruction) {
                NativeCallInstruction callInstruction = (NativeCallInstruction)insn;
                for(int i = 0;i<function.getArgumentNames().length;i++) {
                    mv.visitVarInsn(ALOAD, i+1);
                }
                handleFunctionCall(callInstruction.getNativeLocation(), callInstruction.getCaller().getName(), callInstruction.getCaller().getArgumentNames().length, mv);
            }
        }
        mv.visitLabel(new Label());
        // TODO: Actually compile
    }

    private void handleFunctionCall(String owner, String functionName, int argCount, MethodVisitor mv) {
        Type type = Type.getType(owner.replace('.', '/'));
        Type[] argTypes = new Type[argCount];
        for(int i = 0;i<argTypes.length;i++) {
            argTypes[i] = VALUE_TYPE;
        }
        mv.visitMethodInsn(INVOKESTATIC, type.getInternalName(), functionName, Type.getMethodDescriptor(VALUE_TYPE, argTypes), false);
        //mv.visitInsn(POP); // TODO: remove
    }

    private void buildInitHandles(ClassWriter classWriter, Type classType, ResolvedClass clazz) {
        MethodVisitor mv = classWriter.visitMethod(ACC_PROTECTED, "initHandles", Type.getMethodDescriptor(Type.VOID_TYPE), null, new String[0]);
        mv.visitCode();
        for(ResolvedFunction f : clazz.getFunctions()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(f.getName());
            mv.visitLdcInsn(f.getArgumentNames().length);
            mv.visitMethodInsn(INVOKEVIRTUAL, SET_TYPE.getInternalName(), "registerHandle", Type.getMethodDescriptor(Type.VOID_TYPE, STRING_TYPE, Type.INT_TYPE), false);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void buildConstructor(ClassWriter classWriter, Type classType, ResolvedClass clazz) {
        int access = ACC_PRIVATE;
        if(clazz.isTranslationSet()) {
            access = ACC_PUBLIC;
        }
        MethodVisitor mv = classWriter.visitMethod(access, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), null, new String[0]);
        mv.visitCode();

        mv.visitLabel(new Label());
        if(clazz.isTranslationSet()) {
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
        } else {
            // Call super constructor from Object
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, OBJECT_TYPE.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        }
        mv.visitLabel(new Label());
        // TODO: Cache functions with 0 arguments
        mv.visitMaxs(0, 0); // Let ASM compute maxs and frames
        mv.visitEnd();
    }

    public static Class<? extends TranslationSet> defineClass(String name, byte[] classData) throws InvocationTargetException, IllegalAccessException {
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
                throw new IllegalArgumentException("ThothCompiler does not allow you to define non-Thoth classes");
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
}
