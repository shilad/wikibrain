package org.wikibrain.conf;

public class ConfigurationException extends Exception {
    public ConfigurationException(String message, Exception e) {
        super(message, e);
    }
    public ConfigurationException(Exception e) {
        super(e);
    }
    public ConfigurationException(String message) {
        super(message);
    }
}
