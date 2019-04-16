# InApp purchasing implementation for Google Play

Handles purchases and restores for non-consumable products.

## Deprecation

Please note that this implementation is deprecated. Use [googlebilling](https://github.com/libgdx/gdx-pay/tree/master/gdx-pay-android-googlebilling) instead.

## Configuration

*android:

    dependencies {
        compile "com.badlogicgames.gdxpay:gdx-pay-android:$gdxPayVersion"
        compile "com.badlogicgames.gdxpay:gdx-pay-android-googleplay:${gdxPayVersion}@aar"
    }

* AndroidManifest.xml:
```
<!--all-->
<uses-permission android:name="android.permission.INTERNET"/>
<!--Google Play-->
<uses-permission android:name="com.android.vending.BILLING"/>
<!--Samsung Apps-->
<uses-permission android:name="com.sec.android.iap.permission.BILLING"/>
<!--Nokia-->
<uses-permission android:name="com.nokia.payment.BILLING"/>
<!--SlideME-->
<uses-permission android:name="com.slideme.sam.manager.inapp.permission.BILLING"/>
```
* proguard.cfg:
```
-keep class com.android.vending.billing.**
-keep class com.amazon.** {*;}
-keep class com.sec.android.iap.**
-keep class com.nokia.payment.iap.aidl.**
-keep class com.badlogic.gdx.pay.android.** { *; }
-dontwarn org.onepf.oms.appstore.FortumoBillingService
```

Add this to your `AndroidLauncher`'s `onCreate` method:

    game.purchaseManager = new AndroidGooglePlayPurchaseManager(this, 0);

## Limitations

It implements only features the users have needed so far. The following features are not yet implemented:

* No support for more than 700 purchases per user per app: we do not yet support INAPP_CONTINUATION_TOKEN	
* No support for subscriptions (only supports non-consumable and consumable products currently).

More details on [Status page](STATUS.md) 
