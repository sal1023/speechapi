package com.spokentech.speechdown.server;

public class SynthesisException extends Exception {
    String detail;
    
    public SynthesisException (String message, String detail) {
        super (message);
        this.detail = detail;
    }
    
    public String getDetail () {
        return detail;
    }
}
