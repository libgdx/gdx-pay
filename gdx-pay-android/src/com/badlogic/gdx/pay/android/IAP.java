/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.pay.android;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.backends.android.AndroidEventListener;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseSystem;

import java.lang.reflect.Method;

/**
 * The IAP system for Android supporting Android stores.
 *
 * @author noblemaster
 * @deprecated see {@link PurchaseSystem}
 */
public class IAP implements LifecycleListener, AndroidEventListener {

	/** Debug tag for logging. */
	private static final String TAG = "GdxPay/IAP";

	/**
	 * AndroidApplication will call this dynamically via reflection.
	 * We try to find installed IAP systems (jar-files present). If yes, we use them.
	 */
	public static void setup() {
		try {
			final int requestCode = 1032; // requestCode for onActivityResult for purchases (could go into
// PurchaseManagerConfig)
			Activity activity = null;
			if (Gdx.app instanceof Activity) {
				activity = (Activity) Gdx.app;
			} else {
				Class<?> supportFragmentClass = findClass("android.support.v4.app.Fragment");
				if (supportFragmentClass != null
					&& supportFragmentClass.isAssignableFrom(Gdx.app.getClass())) {
					activity = (Activity) supportFragmentClass.getMethod("getActivity").invoke(Gdx.app);
				} else {
					Class<?> fragmentClass = findClass("android.app.Fragment");
					if (fragmentClass != null && fragmentClass.isAssignableFrom(Gdx.app.getClass())) {
						activity = (Activity) fragmentClass.getMethod("getActivity").invoke(Gdx.app);
					}
				}
			}

			if (activity == null) {
				throw new RuntimeException("Can't find your gdx activity to instantiate Android IAP. "
					+ "Looks like you have implemented AndroidApplication without using "
					+ "Activity or Fragment classes or Activity is not available at the moment");
			}

			IAP iap = new IAP(activity, requestCode);

			// add a listener for Lifecycle events
			Gdx.app.addLifecycleListener(iap);

			// add a listener for Android Events events
			Method gdxAppAddAndroidEventListenerMethod = Gdx.app.getClass().getMethod("addAndroidEventListener",
				AndroidEventListener.class);
			gdxAppAddAndroidEventListenerMethod.invoke(Gdx.app, iap);

			// notify of success
			Gdx.app.log(TAG, "IAP: gdx-pay successfully instantiated.");
		} catch (Exception e) {
			Gdx.app.log(TAG, "IAP: Error creating IAP for Android.", e);
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

	/**
	 * @param activity The AndroidApplication activity.
	 * @param requestCode The request code to use in case they are needed (not all stores need them). */
	public IAP (Activity activity, int requestCode) {
		// are we on GooglePlay?
		try {
			Class<?> googlePlayClazz = Class.forName("com.badlogic.gdx.pay.android.googleplay.AndroidGooglePlayPurchaseManager");
			Method method = googlePlayClazz.getMethod("isRunningViaGooglePlay", Activity.class);
			if ((Boolean)method.invoke(googlePlayClazz, activity)) {
				// we are running on GooglePlay: let's set the purchase manager and be done with it!
				PurchaseSystem.setManager((PurchaseManager)googlePlayClazz.getConstructor(Activity.class, int.class).newInstance(activity, requestCode));
				return;
			}
		} catch (Exception e) {
			Log.d(TAG, "Failed to locate purchase manager for GooglePlay (gdx-pay-android-googleplay.jar file not installed)", e);
		}

	}

	@Override
	public void onActivityResult (int requestCode, int resultCode, Intent data) {
		// forward to corresponding Android IAP-system
		PurchaseManager manager = PurchaseSystem.getManager();
		if (manager != null) {
			try {
				// this might fail which is OK! --> some implementations will not require this...
				Method method = manager.getClass().getMethod("onActivityResult", int.class, int.class, Intent.class);
				method.invoke(manager, requestCode, resultCode, data);
			} catch (Exception e) {
				Log.d(TAG, "Failed to invoke onActivityResult(...) on purchase manager.", e);
			}
		}
	}

	@Override
	public void pause () {
		// not used ...
	}

	@Override
	public void resume () {
		// not used ...
	}

	@Override
	public void dispose () {
		// dispose the purchase system
		PurchaseSystem.dispose();
	}
}
