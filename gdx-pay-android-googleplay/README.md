# InApp purchasing implementation for Google Play

Handles purchases and restores for non-consumable products.

## Unique selling points

* Detailed error reports: all exception messages contain as many details as possible in erroneous situations
* High quality: developed using TDD practices, aiming to deliver a high quality Android payment module.

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

## Contributing

We welcome contributions (bugfixes and new features).

For bugfixes, provide a unit-test that reproduces the bug and verifies it is fixed.

For new features, please provide unit tests for all scenarios that might occur.