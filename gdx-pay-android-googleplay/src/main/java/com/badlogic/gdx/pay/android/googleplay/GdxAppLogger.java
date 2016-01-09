package com.badlogic.gdx.pay.android.googleplay;

import com.badlogic.gdx.Gdx;

public class GdxAppLogger implements Logger {

    private final String tag;

    public GdxAppLogger(String tag) {
        this.tag = tag;
    }

    @Override
    public void debug(String message) {
        Gdx.app.debug(tag, message);
    }

}
