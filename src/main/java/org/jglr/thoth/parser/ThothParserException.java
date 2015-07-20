package org.jglr.thoth.parser;

public class ThothParserException extends Exception {
    public ThothParserException(String message) {
        super(message);
    }

    public ThothParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
