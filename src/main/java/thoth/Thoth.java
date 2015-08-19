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
        String path = fileLocation.getPath();
        String file = path.substring(path.lastIndexOf('/')+1);
        return compile(input, file);
    }

    public ThothClass compile(InputStream fileLocation, String file) throws IOException, ThothParserException {
        String code = Utils.readString(fileLocation, "UTF-8");
        return compile(code, file);
    }

    public ThothClass compile(String rawCode, String file) throws ThothParserException {
        return parser.parseRaw(rawCode, file);
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
