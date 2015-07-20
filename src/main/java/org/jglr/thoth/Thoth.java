package org.jglr.thoth;

import org.jglr.thoth.interpreter.ThothInterpreter;
import org.jglr.thoth.parser.ThothClass;
import org.jglr.thoth.parser.ThothFunc;
import org.jglr.thoth.parser.ThothParser;
import org.jglr.thoth.parser.ThothParserException;

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
        return function;
    }

}
