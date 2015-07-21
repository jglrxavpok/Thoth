import thoth.Constants;
import thoth.Thoth;
import thoth.compiler.JVMCompiler;
import thoth.lang.ThothClass;
import thoth.lang.Translation;
import thoth.lang.TranslationSet;
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
    }
}
