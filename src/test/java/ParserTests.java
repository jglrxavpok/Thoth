import org.junit.Test;
import thoth.compiler.parser.ParsedClass;
import thoth.compiler.parser.ParsedFunction;
import thoth.compiler.parser.ThothParser;
import thoth.compiler.ThothType;

import static org.junit.Assert.*;
import static thoth.testing.ListConditions.*;

public class ParserTests {

    @Test
    public void parseClassName() {
        String thothCode = "class ClassA\n";
        ThothParser parser = new ThothParser(thothCode, "Test.th");
        parser.parse();
        assertEquals("ClassA", parser.getClassName());
        printAllWarningsAndErrors(parser);
    }

    @Test
    public void parseImports() {
        String thothCode = "class ClassA\n" +
                "import ClassB\n" +
                "import ClassC\n" +
                "import ClassC\n";
        ThothParser parser = new ThothParser(thothCode, "Test.th");
        parser.parse();
        assertListEquals(quickList("ClassB", "ClassC", "ClassC"), parser.getImports());
        printAllWarningsAndErrors(parser);
    }

    @Test
    public void testComments() {
        String thothCode = "class ClassA\n" +
                "// One line comment\n" +
                "/*import ClassC\n" +
                "import ClassC\n" +
                "Multiline comment*/\n";
        ThothParser parser = new ThothParser(thothCode, "Test.th");
        parser.parse();
        assertListEquals(quickList(), parser.getImports());
        printAllWarningsAndErrors(parser);
    }

    @Test
    public void parseUserTypes() {
        String thothCode = "class ClassA\n" +
                "typedef MyTypeA a\n" +
                "typedef MyTypeB b\n";
        ThothParser parser = new ThothParser(thothCode, "Test.th");
        parser.parse();
        assertListEquals(quickList(new ThothType("MyTypeA", "a"), new ThothType("MyTypeB", "b")), parser.getUserDefinedTypes());
        printAllWarningsAndErrors(parser);
    }

    @Test
    public void parseTypelessAndParamlessFunction() {
        String thothCode = "class ClassA\n" +
                "def test = \"MyCode\"\n";
        ThothParser parser = new ThothParser(thothCode, "Test.th");
        parser.parse();
        assertEquals(1, parser.getFunctions().size());
        ParsedFunction function = parser.getFunctions().get(0);
        assertEquals("test", function.getName());
        assertArrayEquals(new String[0], function.getTypes());
        assertArrayEquals(new String[0], function.getArgumentNames());
        assertEquals("\"MyCode\"", function.getCode());
        printAllWarningsAndErrors(parser);
    }

    @Test
    public void parseTypelessFunction() {
        String thothCode = "class ClassA\n" +
                "def test(arg1, arg2) = \"MyCode\"\n";
        ThothParser parser = new ThothParser(thothCode, "Test.th");
        parser.parse();
        assertEquals(1, parser.getFunctions().size());
        ParsedFunction function = parser.getFunctions().get(0);
        assertEquals("test", function.getName());
        assertArrayEquals(new String[0], function.getTypes());
        assertArrayEquals(new String[]{"arg1", "arg2"}, function.getArgumentNames());
        assertEquals("\"MyCode\"", function.getCode());
        printAllWarningsAndErrors(parser);
    }

    @Test
    public void parseParamlessFunction() {
        String thothCode = "class ClassA\n" +
                "def test:TestType = \"MyCode\"\n";
        ThothParser parser = new ThothParser(thothCode, "Test.th");
        parser.parse();
        assertEquals(1, parser.getFunctions().size());
        ParsedFunction function = parser.getFunctions().get(0);
        assertEquals("test", function.getName());
        assertArrayEquals(new String[]{"TestType"}, function.getTypes());
        assertArrayEquals(new String[0], function.getArgumentNames());
        assertEquals("\"MyCode\"", function.getCode());
        printAllWarningsAndErrors(parser);
    }

    @Test
    public void parseSingleFunction() {
        String thothCode = "class ClassA\n" +
                "def test(arg1, arg2):TestType = \"MyCode\"\n";
        ThothParser parser = new ThothParser(thothCode, "Test.th");
        parser.parse();
        assertEquals(1, parser.getFunctions().size());
        ParsedFunction function = parser.getFunctions().get(0);
        assertEquals("test", function.getName());
        assertArrayEquals(new String[]{"TestType"}, function.getTypes());
        assertArrayEquals(new String[]{"arg1", "arg2"}, function.getArgumentNames());
        assertEquals("\"MyCode\"", function.getCode());
        printAllWarningsAndErrors(parser);
    }

    @Test
    public void parseSingleMultilineFunction() {
        String thothCode = "class ClassA\n" +
                "def test(arg1, arg2):TestType = {\n" +
                "\"MyCode\" arg1\n" +
                "}\n";
        ThothParser parser = new ThothParser(thothCode, "Test.th");
        parser.parse();
        assertEquals(1, parser.getFunctions().size());
        ParsedFunction function = parser.getFunctions().get(0);
        assertEquals("test", function.getName());
        assertArrayEquals(new String[]{"TestType"}, function.getTypes());
        assertArrayEquals(new String[]{"arg1", "arg2"}, function.getArgumentNames());
        assertEquals("{\n" + "\"MyCode\" arg1\n" + "}", function.getCode());
        printAllWarningsAndErrors(parser);
    }

    private void printAllWarningsAndErrors(ThothParser parser) {
        parser.getWarnings().forEach(Throwable::printStackTrace);
        parser.getErrors().forEach(Throwable::printStackTrace);
        ParsedClass clazz = parser.getParsedClass();
        if(clazz != null) {
            System.out.println(clazz.rebuildSource());
        }
    }
}
