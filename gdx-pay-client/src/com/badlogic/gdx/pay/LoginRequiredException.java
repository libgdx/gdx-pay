package com.badlogic.gdx.pay;

public class LoginRequiredException extends GdxPayException {
    public LoginRequiredException() {
        super("Installation failed - Login required.");
    }
}