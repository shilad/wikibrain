package org.wikapidia.parser;


public class WpParseException extends Exception {
    public WpParseException(Exception e) {
        super(e);
    }
    public WpParseException(String message) {
        super(message);
    }
}
