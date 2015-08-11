/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.pay;

import java.lang.reflect.Method;

/** Our purchase system to make InApp payments.
 *
 * @author noblemaster */
public final class PurchaseSystem {

    private static final String TAG = "IAP";

    /** The actual purchase manager or null if none was available. */
    private static PurchaseManager manager = null;

    private PurchaseSystem () {
        // private to prevent instantiation
    }

    /** Registers a new purchase manager. */
    public static void setManager (PurchaseManager manager) {
        PurchaseSystem.manager = manager;
    }

    /** We try to locate a suitable store via Java reflection. */
    private static void resolve () {
        // obtain the Gdx class
        try {
            Class<?> gdxClazz = Class.forName("com.badlogic.gdx.Gdx");
            Class<?> gdxLifecycleListenerClazz = Class.forName("com.badlogic.gdx.LifecycleListener");
            Object gdxAppObject = gdxClazz.getField("app").get(null);
            Method gdxAppLogMethod = gdxAppObject.getClass().getMethod("log", String.class, String.class);
            Method gdxAppLogMethodT = gdxAppObject.getClass().getMethod("log", String.class, String.class, Throwable.class);
            Method gdxAppAddLifecycleListenerMethod = gdxAppObject.getClass().getMethod("addLifecycleListener",
                gdxLifecycleListenerClazz);

            // check if we are on iOS
            boolean ios = findClass("com.badlogic.gdx.backends.iosrobovm.IOSApplication") != null;
            if (ios) {
                try {
                    // look for gdx-pay-iosrobovm and if it exists, instantiate it (gdx-pay jars need to be in place)
                    Class<?> iapClazz = Class.forName("com.badlogic.gdx.pay.ios.apple.PurchaseManageriOSApple");
                    PurchaseSystem.setManager((PurchaseManager)iapClazz.newInstance());

                    // notify of success
                    gdxAppLogMethod.invoke(gdxAppObject, TAG, "IAP: gdx-pay successfully instantiated.");
                } catch (Exception e) {
                    // some jar files appear to be missing
                    gdxAppLogMethodT.invoke(gdxAppObject, TAG,
                        "IAP: Error creating IAP for iOS (are the gdx-pay**.jar files installed?).", e);
                }
                return;
            }

            // check if we are on Android
            boolean android = findClass("com.badlogic.gdx.backends.android.AndroidApplication") != null;
            if (android) {
                try {
                    // look for gdx-pay-android and if it exists, instantiate it (gdx-pay jars need to be in place)
                    Class<?> iapClazz = Class.forName("com.badlogic.gdx.pay.android.IAP");
                    Class<?> activityClazz = Class.forName("android.app.Activity");
                    Class<?> intentClazz = Class.forName("android.content.Intent");
                    Class<?> gdxAndroidEventListenerClazz = Class
                        .forName("com.badlogic.gdx.backends.android.AndroidEventListener");
                    int requestCode = 1032; // requestCode for onActivityResult for purchases (could go into
// PurchaseManagerConfig)
                    Object activity = null;
                    if (activityClazz.isAssignableFrom(gdxAppObject.getClass())) {
                        activity = gdxAppObject;
                    } else {
                        Class<?> supportFragmentClass = findClass("android.support.v4.app.Fragment");
                        if (supportFragmentClass != null
                            && supportFragmentClass.isAssignableFrom(gdxAppObject.getClass())) {
                            activity = supportFragmentClass.getMethod("getActivity").invoke(gdxAppObject);
                        } else {
                            Class<?> fragmentClass = findClass("android.app.Fragment");
                            if (fragmentClass != null && fragmentClass.isAssignableFrom(gdxAppObject.getClass())) {
                                activity = fragmentClass.getMethod("getActivity").invoke(gdxAppObject);
                            }
                        }
                    }

                    if (activity == null) {
                        throw new RuntimeException("Can't find your gdx activity to instantiate Android IAP. "
                            + "Looks like you have implemented AndroidApplication without using "
                            + "Activity or Fragment classes or Activity is not available at the moment");
                    }

                    Object iap = iapClazz.getConstructor(activityClazz, int.class).newInstance(activity, requestCode);

                    // add a listener for Lifecycle events
                    gdxAppAddLifecycleListenerMethod.invoke(gdxAppObject, iap);

                    // add a listener for Android Events events
                    Method gdxAppAddAndroidEventListenerMethod = gdxAppObject.getClass().getMethod("addAndroidEventListener",
                        gdxAndroidEventListenerClazz);
                    gdxAppAddAndroidEventListenerMethod.invoke(gdxAppObject, iap);

                    // notify of success
                    gdxAppLogMethod.invoke(gdxAppObject, TAG, "IAP: gdx-pay successfully instantiated.");
                } catch (Exception e) {
                    // some jar files appear to be missing
                    gdxAppLogMethodT.invoke(gdxAppObject, TAG,
                        "IAP: Error creating IAP for Android (are the gdx-pay**.jar files installed?).", e);
                }
                return;
            }

            // notify not "reflection"
            gdxAppLogMethod.invoke(gdxAppObject, TAG, "IAP: gdx-pay not instantiated via reflection.");
        } catch (Exception e) {
            // we appear not to be on libGDX!
        }
    }

    /** @return null if class is not available in runtime */
    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the registered manager or null for none. */
    public static PurchaseManager getManager () {
        // resolve our manager via reflection if we do not have one
        if (manager == null) {
            resolve();
        }

        // return the manager or null if none was found
        return manager;
    }

    /** Returns true if there is a purchase manager available. */
    public static boolean hasManager () {
        return getManager() != null;
    }

    /** Returns the store name or null for none. */
    public static String storeName () {
        if (hasManager()) {
            return manager.storeName();
        } else {
            return null;
        }
    }

    /** @see #install(PurchaseObserver, PurchaseManagerConfig, boolean) */
    public static void install(PurchaseObserver observer, PurchaseManagerConfig config) {
        install(observer, config, true);
    }

    /**
     * Installs a purchase observer.
     *
     * @param autoFetchInformation tells PurchaseManager to automatically fetch offer details on setup to make {@link
     * PurchaseSystem#getInformation(String)} work properly *
     */
    public static void install(PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
        if (hasManager()) {
            manager.install(observer, config, autoFetchInformation);
        } else {
            observer.handleInstallError(new RuntimeException("No purchase manager was available."));
        }
    }

    /** Returns true if the purchase system is installed and ready to go. */
    public static boolean installed () {
        if (hasManager()) {
            return manager.installed();
        } else {
            return false;
        }
    }

    /** Disposes the purchase manager if there was one. */
    public static void dispose () {
        if (hasManager()) {
            manager.dispose();
            manager = null;
        }
    }

    /** Executes a purchase. */
    public static void purchase (String identifier) {
        if (hasManager()) {
            manager.purchase(identifier);
        } else {
            throw new RuntimeException("No purchase manager was found.");
        }
    }

    /** Asks to restore previous purchases. Results are returned to the observer. */
    public static void purchaseRestore () {
        if (hasManager()) {
            manager.purchaseRestore();
        } else {
            throw new RuntimeException("No purchase manager was found.");
        }
    }

    /**
     * Returns information about a product provided by the purchase manager.
     * <p/>
     * Note, you should set autoFetchInformation to true in {@link PurchaseSystem#install} to true to make this method
     * work for all PurchaseManager implementations
     *
     * @return {@link Information#UNAVAILABLE} if the product is not available or information was not previously fetched
     */
    public static Information getInformation(String identifier) {
        if (hasManager()) {
            return manager.getInformation(identifier);
        } else {
            throw new RuntimeException("No purchase manager was found.");
        }
    }
}
