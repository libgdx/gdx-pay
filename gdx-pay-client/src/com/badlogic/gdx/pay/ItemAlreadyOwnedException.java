package com.badlogic.gdx.pay;

/**
 * Thrown by a purchase on an item that is already owned
 *
 * Created by Benjamin Schulte on 08.07.2018.
 */

public class ItemAlreadyOwnedException extends GdxPayException {

    public ItemAlreadyOwnedException() {
        super("Purchase failed: Item is already owned.");
    }
}
