
package com.badlogic.gdx.pay.tests;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;

public class Main extends IOSApplication.Delegate {
    @Override
    protected IOSApplication createApplication () {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        config.orientationLandscape = true;
        config.orientationPortrait = false;
        config.useAccelerometer = false;
        config.allowIpod = true;

        return new IOSApplication(new PayTest(), config);
    }

    public static void main (String[] args) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(args, null, Main.class);
        pool.close();
    }
}
