package thoth;

import thoth.interpreter.ThothInterpreter;
import thoth.lang.ThothValue;
import thoth.lang.ThothClass;
import thoth.lang.ThothFunc;
import thoth.parser.ThothParser;
import thoth.parser.ThothParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Thoth {

    public final static Thoth singleton = new Thoth();
    private final ThothInterpreter interpreter;
    private final ThothParser parser;

    private Thoth() {
        parser = new ThothParser();
        interpreter = new ThothInterpreter();
    }

    public ThothClass compile(URL fileLocation) throws IOException, ThothParserException {
        InputStream input = fileLocation.openStream();
        return compile(input);
    }

    public ThothClass compile(InputStream fileLocation) throws IOException, ThothParserException {
        String code = Utils.readString(fileLocation, "UTF-8");
        return compile(code);
    }

    public ThothClass compile(String rawCode) throws ThothParserException {
        return parser.parseRaw(rawCode);
    }

    public String interpret(ThothFunc func, ThothValue... parameters) {
        return interpreter.interpret(func, parameters);
    }

    public String interpret(ThothClass clazz, String function, ThothValue... parameters) {
        ThothFunc func = clazz.getFunction(function);
        if(func != null) {
            return interpret(func, parameters);
        }
        return "Function not found: "+function;
    }

}
