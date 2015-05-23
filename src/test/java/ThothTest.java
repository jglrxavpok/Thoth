import org.jglr.thoth.*;
import org.junit.Assert;
import org.junit.Test;

public class ThothTest implements Constants {

    @Test
    public void simpleReturn() throws ThothParserException {
        new ThothParser().parseRaw("def foo:fp=bar|end|");
    }

    @Test
    public void simpleInterpret() throws ThothParserException {
        ThothParser parser = new ThothParser();
        ThothClass clazz = parser.parseRaw("def foo:fp=bar|end|");
        ThothFunc func = clazz.getFunction("foo");
        new ThothInterpreter().interpret(func);
    }

    @Test
    public void inline() throws ThothParserException {
        ThothParser parser = new ThothParser();
        ThothClass clazz = parser.parseRaw("def foo(a)=Inlined text is \"|a|\"|end|");
        ThothFunc func = clazz.getFunction("foo");
        ThothValue arg = new ThothValue(ThothValue.Types.TEXT, "foobar");
        String result = new ThothInterpreter().interpret(func, arg);
        Assert.assertTrue("Failed inlining: "+result, result.equals("Inlined text is \"foobar\""));
    }

    @Test
    public void nullArgs() throws ThothParserException {
        ThothParser parser = new ThothParser();
        ThothClass clazz = parser.parseRaw("def foo(a,b,c)=|a->fp?{|If you see this, it means something has gone terribly wrong ;(|}||end|");
        ThothFunc func = clazz.getFunction("foo");
        String result = new ThothInterpreter().interpret(func);
        Assert.assertTrue("'foo' function is called with null arguments, condition should fail", result.isEmpty());
    }

    @Test
    public void customArgs() throws ThothParserException {
        ThothParser parser = new ThothParser();
        ThothClass clazz = parser.parseRaw("def foo(a,b,c)=" +
                "|a->fp?{|" +
                    "If you see this, it means the condition worked!" +
                "|}|" +
                " I'm a text that doesn't need conditions!" +
                "|end|");
        Translation tr = new Translation(FLAG_FEMININE | FLAG_PLURAL, "Tests", new String[0]);
        ThothValue a = new ThothValue(ThothValue.Types.TRANSLATION, tr);
        ThothFunc func = clazz.getFunction("foo");
        String result = new ThothInterpreter().interpret(func, a);
        Assert.assertTrue("'foo' function is called with custom arguments, condition shouldn't fail", result.equals("If you see this, it means the condition worked!"+
                " I'm a text that doesn't need conditions!"));
    }

    @Test
    public void params() throws ThothParserException {
        new ThothParser().parseRaw("def foo(a,b,c)=|a->fp?||end|");
    }

}
