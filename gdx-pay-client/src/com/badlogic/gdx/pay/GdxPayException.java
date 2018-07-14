package com.badlogic.gdx.pay;

public class GdxPayException extends RuntimeException {

    public GdxPayException(String message, Exception rootCause) {
        super(message, rootCause);
    }

    public GdxPayException(String message) {
        super(message);
    }

}
