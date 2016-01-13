package com.badlogic.gdx.pay.android.googleplay;

public class GdxPayException extends RuntimeException {

    public GdxPayException(String message, Exception rootCause) {
        super(message, rootCause);
    }

    public GdxPayException(String message) {
        super(message);
    }
}
