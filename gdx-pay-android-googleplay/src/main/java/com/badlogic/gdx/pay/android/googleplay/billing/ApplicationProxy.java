package com.badlogic.gdx.pay.android.googleplay.billing;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidEventListener;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;

public interface ApplicationProxy {
    String getPackageName();

    void addAndroidEventListener(AndroidEventListener listener);
    void removeAndroidEventListener(AndroidEventListener listener);

    void unbindService(ServiceConnection conn);
    boolean bindService(Intent bindBillingServiceIntent,
                        ServiceConnection billingServiceConnection,
                        int bindAutoCreate);

    void startIntentSenderForResult(IntentSender intentSender,
                                    int activityRequestCode,
                                    Intent intent,
                                    int flagsMask,
                                    int flagsValues,
                                    int extraFlags) throws IntentSender.SendIntentException;

    class ActivityProxy implements ApplicationProxy {
        private final AndroidApplication application;

        public ActivityProxy(AndroidApplication application) {
            this.application = application;
        }

        @Override
        public String getPackageName() {
            return application.getPackageName();
        }

        @Override
        public void addAndroidEventListener(AndroidEventListener listener) {
            application.addAndroidEventListener(listener);
        }

        @Override
        public void removeAndroidEventListener(AndroidEventListener listener) {
            application.removeAndroidEventListener(listener);
        }

        @Override
        public void unbindService(ServiceConnection conn) {
            application.unbindService(conn);
        }

        @Override
        public boolean bindService(Intent bindBillingServiceIntent,
                                   ServiceConnection billingServiceConnection,
                                   int bindAutoCreate) {
            return bindService(bindBillingServiceIntent, billingServiceConnection, bindAutoCreate);
        }

        @Override
        public void startIntentSenderForResult(IntentSender intentSender,
                                               int activityRequestCode,
                                               Intent intent,
                                               int flagsMask,
                                               int flagsValues,
                                               int extraFlags) throws IntentSender.SendIntentException {

            application. startIntentSenderForResult(intentSender, activityRequestCode, intent,
                flagsMask, flagsValues, extraFlags);
        }
    }

    class FragmentProxy implements ApplicationProxy {
        private Activity activity;
        private final AndroidFragmentApplication application;

        public FragmentProxy(Activity activity, AndroidFragmentApplication application) {
            this.activity = activity;
            this.application = application;
        }

        @Override
        public String getPackageName() {
            return activity.getPackageName();
        }

        @Override
        public void addAndroidEventListener(AndroidEventListener listener) {
            application.addAndroidEventListener(listener);
        }

        @Override
        public void removeAndroidEventListener(AndroidEventListener listener) {
            application.removeAndroidEventListener(listener);
        }

        @Override
        public void unbindService(ServiceConnection conn) {
            application.getContext().unbindService(conn);
        }

        @Override
        public boolean bindService(Intent bindBillingServiceIntent,
                                   ServiceConnection billingServiceConnection,
                                   int bindAutoCreate) {

            return activity.bindService(bindBillingServiceIntent, billingServiceConnection, bindAutoCreate);
        }

        @Override
        public void startIntentSenderForResult(IntentSender intentSender,
                                               int activityRequestCode,
                                               Intent intent,
                                               int flagsMask,
                                               int flagsValues,
                                               int extraFlags) throws IntentSender.SendIntentException {

            activity.startIntentSenderForResult(intentSender, activityRequestCode, intent,
                flagsMask, flagsValues, extraFlags);
        }
    }
}

