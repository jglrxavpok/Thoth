import thoth.Constants;
import thoth.Thoth;
import thoth.compiler.JVMCompiler;
import thoth.lang.*;
import thoth.parser.ThothParserException;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class ThothCompilerTests implements Constants {

    @Test
    public void jvmCompile() throws IOException, ThothParserException, InvocationTargetException, IllegalAccessException, InstantiationException {
        JVMCompiler compiler = new JVMCompiler();
        File file = new File("./testscompiled/thothtest/", "TestClass.class");
        if(!file.getParentFile().exists())
            file.getParentFile().mkdirs();
        if(file.exists())
            file.delete();
        file.createNewFile();
        byte[] code = compiler.compile(getClass().getResource("test.th"), true);
        FileOutputStream out = new FileOutputStream(file);
        out.write(code);
        out.flush();
        out.close();

        Class<? extends TranslationSet> setClass = compiler.defineClass("thothtest.TestClass", code);
        TranslationSet set = setClass.newInstance();
        System.out.println(set.getTranslation("frenchHelloWorld"));
        System.out.println(set.getTranslation("test1", Translation.create("Test0", FLAG_NEUTRAL), Translation.create("Test2", FLAG_NEUTRAL)));
        System.out.println(set.getTranslation("foo2", Translation.create("Test0")));
        System.out.println(set.getTranslation("foo2", new NullValue()));
        System.out.println(set.getTranslation("foo2", new ThothValue(ThothValue.Types.TEXT, null)));
        System.out.println(set.getTranslation("foo2", new ThothValue(ThothValue.Types.BOOL, true)));
        System.out.println(set.getTranslation("elseTest", Translation.create("Test0")));
        System.out.println(set.getTranslation("elseTest", new NullValue()));
        System.out.println(set.getTranslation("funcTest", new NullValue()));
        System.out.println(set.getTranslation("varfuncTest", Translation.create("frenchHelloWorld")));
    }
}
