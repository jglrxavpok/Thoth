import org.junit.Test;
import thoth.Utils;
import thoth.compiler.ThothCompilePhase;
import thoth.compiler.ThothType;
import thoth.compiler.bytecode.ThothCompiler;
import thoth.compiler.parser.ParsedClass;
import thoth.compiler.parser.ThothParser;
import thoth.compiler.resolver.ResolvedClass;
import thoth.compiler.resolver.ThothResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class CompilerTests {

    @Test
    public void simpleCompile() throws IOException {
        String thothCode = "class ClassA\n" +
                "import thoth.lang.ThothTypes\n" +
                "def test(a, b):Neutral = toUpperCase(\"MyCode\") a\n";
        ThothParser parser = new ThothParser(thothCode, "ClassA.th");
        parser.parse();
        printAllWarningsAndErrors(parser);
        ParsedClass clazz = parser.getParsedClass();
        ThothResolver resolver = ThothResolver.createWithDefaultClasses(clazz);
        resolver.resolve();
        printAllWarningsAndErrors(resolver);
        ResolvedClass resolvedClass = resolver.getResolvedClasses()[0];
        ThothCompiler compiler = new ThothCompiler();
        for(ResolvedClass baseClass : ThothResolver.getBaseClasses()) {
            byte[] bytecode = compiler.compile(baseClass);
            File destinationFile = new File(".", "testcompilation/"+baseClass.getName().replace('.', '/')+".class");
            if(!destinationFile.getParentFile().exists()) {
                destinationFile.getParentFile().mkdirs();
            }
            FileOutputStream out = new FileOutputStream(destinationFile);
            out.write(bytecode);
            out.flush();
            out.close();
        }

        byte[] bytecode = compiler.compile(resolvedClass);
        FileOutputStream out = new FileOutputStream(new File(".", "ClassA.class"));
        out.write(bytecode);
        out.flush();
        out.close();
    }

    private void printAllWarningsAndErrors(ThothCompilePhase parser) {
        parser.getWarnings().forEach(Throwable::printStackTrace);
        parser.getErrors().forEach(Throwable::printStackTrace);
    }
}
