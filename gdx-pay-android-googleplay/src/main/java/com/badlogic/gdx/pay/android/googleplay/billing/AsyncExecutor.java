package com.badlogic.gdx.pay.android.googleplay.billing;

public interface AsyncExecutor {

    void executeAsync(Runnable runnable, long delayInMs);
}
