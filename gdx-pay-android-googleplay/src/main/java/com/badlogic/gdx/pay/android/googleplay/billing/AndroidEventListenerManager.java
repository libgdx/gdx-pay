package com.badlogic.gdx.pay.android.googleplay.billing;

import com.badlogic.gdx.backends.android.AndroidEventListener;

class AndroidEventListenerManager {

    private volatile boolean isListening;

    private final ApplicationProxy proxy;
    private final AndroidEventListener androidEventListener;

    public AndroidEventListenerManager(ApplicationProxy proxy, AndroidEventListener AndroidEventListener) {
        this.proxy = proxy;
        androidEventListener = AndroidEventListener;
    }

    void addListenerOnce() {
        if (!isListening) {
            proxy.addAndroidEventListener(androidEventListener);
            isListening = true;
        }
    }

    void removeListenerOnce() {
        if (isListening) {
            proxy.removeAndroidEventListener(androidEventListener);
            isListening = false;

        }
    }
}
