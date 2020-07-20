package com.badlogic.gdx.pay;

public class RegionNotSupportedException extends GdxPayException {
    public RegionNotSupportedException() {
        super("Installation failed - Region not supported.");
    }
}