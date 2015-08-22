import org.junit.Test;
import thoth.Utils;
import thoth.compiler.ThothCompilePhase;
import thoth.compiler.ThothType;
import thoth.compiler.parser.ParsedClass;
import thoth.compiler.parser.ParsedFunction;
import thoth.compiler.parser.ThothParser;
import thoth.compiler.resolver.ResolvedClass;
import thoth.compiler.resolver.ThothResolver;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static thoth.testing.ListConditions.assertListEquals;
import static thoth.testing.ListConditions.quickList;

public class ResolverTests {

    @Test
    public void resolveImportAndType() {
        String thothCode = "class ClassA\n" +
                "import thoth.lang.ThothTypes\n" +
                "def test:Neutral = \"MyCode\"\n";
        ThothParser parser = new ThothParser(thothCode, "Test.th");
        parser.parse();
        printAllWarningsAndErrors(parser);
        ParsedClass clazz = parser.getParsedClass();
        ThothResolver resolver = ThothResolver.createWithDefaultClasses(clazz);
        resolver.resolve();
        printAllWarningsAndErrors(resolver);
        ResolvedClass resolvedClass = resolver.getResolvedClasses()[0];
        assertArrayEquals(new ThothType[]{new ThothType("Neutral", "n")}, resolvedClass.getFunctions().get(0).getTypes());
    }

    @Test
    public void resolveTypeFromOther() throws IOException {
        String class1 = Utils.readString(ResolverTests.class.getResourceAsStream("/TestClass.th"), "UTF-8");
        String class2 = Utils.readString(ResolverTests.class.getResourceAsStream("/TestClass2.th"), "UTF-8");
        ThothParser parserClass1 = new ThothParser(class1, "TestClass.th");
        ThothParser parserClass2 = new ThothParser(class2, "TestClass2.th");
        parserClass1.parse();
        parserClass2.parse();
        printAllWarningsAndErrors(parserClass1);
        printAllWarningsAndErrors(parserClass2);
        ThothResolver resolver = ThothResolver.createWithDefaultClasses(parserClass1.getParsedClass(), parserClass2.getParsedClass());
        resolver.resolve();
        printAllWarningsAndErrors(resolver);
    }

    private void printAllWarningsAndErrors(ThothCompilePhase parser) {
        parser.getWarnings().forEach(Throwable::printStackTrace);
        parser.getErrors().forEach(Throwable::printStackTrace);
    }
}
