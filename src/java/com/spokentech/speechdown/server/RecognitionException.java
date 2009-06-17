package com.spokentech.speechdown.server;

public class RecognitionException extends Exception {
    String detail;
    
    public RecognitionException (String message, String detail) {
        super (message);
        this.detail = detail;
    }
    
    public String getDetail () {
        return detail;
    }
}
