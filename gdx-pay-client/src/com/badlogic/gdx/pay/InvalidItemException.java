package com.badlogic.gdx.pay;

/**
 * Thrown by a purchase on an item that is invalid or unavailable
 */
public class InvalidItemException extends GdxPayException {
    public InvalidItemException() {
        this("");
    }

    public InvalidItemException(String identifier) {
        super("Purchase failed, invalid product identifier " + identifier);
    }
}
