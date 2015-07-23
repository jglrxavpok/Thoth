import thoth.Constants;
import thoth.Thoth;
import thoth.lang.ThothClass;
import thoth.parser.ThothParserException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ThothHiLevelTests implements Constants {

    @Test
    public void simpleCompile() throws IOException, ThothParserException {
        ThothClass clazz = Thoth.singleton.compile(getClass().getResource("test.th"));
        Assert.assertTrue("Class was not compiled correctly: function list is empty", !clazz.getFunctions().isEmpty());
    }

    @Test
    public void simpleInterpret() throws IOException, ThothParserException {
        ThothClass clazz = Thoth.singleton.compile(getClass().getResource("test.th"));
        clazz.getFunctions().forEach(f -> System.out.println(f.getName()));
        String result = Thoth.singleton.interpret(clazz, "test0");
        Assert.assertTrue("Function call on 'test0' did not return expected results, got \""+result+"\" instead",
                result.equals("Hello world!"));
    }

    @Test
    public void simpleElse() throws IOException, ThothParserException {
        ThothClass clazz = Thoth.singleton.compile(getClass().getResource("test.th"));
        clazz.getFunctions().forEach(f -> System.out.println(f.getName()));
        String result = Thoth.singleton.interpret(clazz, "elseTest");
        Assert.assertTrue("Function call on 'elseTest' did not return expected results, got \""+result+"\" instead",
                result.equals("Null :c"));
    }
}
