[![Build Status](https://travis-ci.org/libgdx/gdx-pay.svg?branch=master)](https://travis-ci.org/libgdx/gdx-pay)

This project aims to provide a **cross-platform API for InApp purchasing**.
The gdx-pay project is a libGDX extension. Current release version is 0.10.2. Please use at least libGDX v1.9.1 and robovm v1.13.0.

### Getting Started

The purchasing API comes in two parts.

* **Client-Side API**: This is what is integrated into your game or application and will handle the
purchase flow for the application.

* **Server-Side API (optional)**: If you have a server running and would like to do a purchase verification
on your server, you can use the API to verify purchases sent to your server. 

The recommended way to use gdx-pay is via dependency management with Gradle or Maven. Artifacts are available in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.badlogicgames.gdxpay%22). [Guide for using gdx-pay with Maven](https://github.com/libgdx/gdx-pay/wiki/Maven-Integration).

#### Client-Side API

To setup the purchasing API, you will need to add the corresponding components in your. In your **core project** you have:
* [gdx-pay.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay/0.10.2/gdx-pay-0.10.2-library.jar)
* [gdx-pay-client.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-client/0.10.2/gdx-pay-client-0.10.2-library.jar)

In your **Android project** you use:
* [gdx-pay-android.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-android/0.10.2/gdx-pay-android-0.10.2-library.jar)
* [gdx-pay-android-googleplay.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-android-googleplay/0.10.2/gdx-pay-android-googleplay-0.10.2.jar) ( for Google Play with non-consumable products only, exclude this artifact when using other product types. [See status](gdx-pay-android-googleplay/README.md))
* [gdx-pay-android-openiab.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-android-openiab/0.10.2/gdx-pay-android-openiab-0.10.2-library.jar) (to support GooglePlay, Amazon etc. [This component is deprecated!](gdx-pay-android-openiab/README.md) Do not use if you are using gdx-pay-android-googleplay.jar, they are mutually exclusive.))
* [gdx-pay-android-ouya.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-android-ouya/0.10.2/gdx-pay-android-ouya-0.10.2-library.jar) (to support OUYA)
* AndroidManifest.xml: 
```
<!--all-->
<uses-permission android:name="android.permission.INTERNET"/>
<!--Google Play-->
<uses-permission android:name="com.android.vending.BILLING"/>
<!--Open Store-->
<uses-permission android:name="org.onepf.openiab.permission.BILLING"/>
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
-dontwarn org.onepf.oms.appstore.FortumoBillingService
```

In your **iOS project** you use:
* [gdx-pay-iosrobovm-apple.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-iosrobovm-apple/0.10.2/gdx-pay-iosrobovm-apple-0.10.2-library.jar)
* robovm.xml: add `<pattern>com.badlogic.gdx.pay.ios.apple.PurchaseManageriOSApple</pattern>` inside `<forceLinkClasses>...</forceLinkClasses>`.

In your **Desktop project** you use:
* [gdx-pay-desktop-apple.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-desktop-apple/0.10.2/gdx-pay-desktop-apple-0.10.2-library.jar) (to support the Mac App Store): *needs implementation/volunteers wanted!*

In your **GWT project** you use:
* [gdx-pay-gwt-googlewallet.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-gwt-googlewallet/0.10.2/gdx-pay-gwt-googlewallet-0.10.2-library.jar) (to support Google Wallet): *needs implementation/volunteers wanted!*

In any case, if the correct jar files are place, all you need is to initialize the purchase system in your 
core project as follows.


```
...
// Disposes static instances in case JVM is re-used on restarts
PurchaseSystem.onAppRestarted();

//If gdx-pay is called too early, a manager may not be registered which would be ONE of the reasons for hasManager to return false
if (PurchaseSystem.hasManager()) {
    

  // purchase system is ready to start. Let's initialize our product list etc...
  PurchaseManagerConfig config = new PurchaseManagerConfig();
  //add items that can be purchased in the store, they must be listed before being purchased.
  config.addOffer(new Offer().setType(OfferType.ENTITLEMENT).setIdentifier("ExamplePurchase"));
  config.addOffer(new Offer().setType(OfferType.CONSUMABLE).setIdentifier("ExampleConsumablePurchase"));
  ...
  //add any stores you are planning on using (Note, IOS_APPLE doesn't actually have an encoded key so pass any string as the second parameter)
  config.addStoreParam(PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE, "<Google key>");
  
  config.addStoreParam(PurchaseManagerConfig.STORE_NAME_ANDROID_OUYA, new Object[] { 
    OUYA_DEVELOPER_ID, 
    KEYPATH 
  });
  ...

  // let's start the purchase system...
  PurchaseSystem.install(new PurchaseObserver() {         
   ...
   //implement all needed methods.  Check for purchases in handleRestore and handlePurchase using the transactions argument. // e.g:  if(transactions[i].getIdentifier().equals("ExamplePurchase"))
  }, config);
  ...
  
  // to make a purchase (results are reported to the observer)
  PurchaseSystem.purchase("product_identifier"); 
  ...
    
  // (*) to restore existing purchases (results are reported to the observer)
  PurchaseSystem.restore();
  ...
  
  // obtain localized product information (not supported by all platforms)
  Information information = PurchaseSystem.getInformation("product_identifier");
  ...
}
...
```

`(*)` IMPORTANT: `PurchaseSystem.restore()' should *not be called directly* by your application. Restoring purchases shall only be 
called when a user explicitly requests it. In your application add a [Restore Purchases] button which in turn will call this method.
This is a requirement by Apple iOS. If you don't provide a button for purchase restores your application will be rejected! You have
been warned :)

Please note: Gdx-Pay does not work on android emulators at all and will run on apple emulators but they will not be able to complete any purchases.

For keys you have to pass in have a look at [OpenIAB's Sample Configuration](https://github.com/onepf/OpenIAB/blob/dev/samples/trivialdrive/src/main/java/org/onepf/sample/trivialdrive/InAppConfig.java).
Please note you will not need to pass in any keys for Amazon as it doesn't require them.

Here is a user-guide in the wiki on how to implement gdx-pay in your libgdx project with using platform resolvers: 
 [Integration-example-with-resolvers](https://github.com/libgdx/gdx-pay/wiki/Integration-example-with-resolvers)

The gdx-pay-android-googleplay implementation supports cancellation of test purchases:

    PurchaseManager mananager = PurchaseSystem.getManager();
    if (manager instanceof PurchaseManagerTestSupport) {
        ((PurchaseManagerTestSupport) manager).cancelTestPurchases();
    }

#### Server-Side API (Optional)

**gdx-pay-server** is optional and can be used for purchase **verification on a server**. It verifies if a 
purchase is valid by e.g. doing a post-back validation on a server. 
Place the following two jar-files onto your server (you won't need any other libGDX 
libraries on your server, all dependencies to libGDX have been removed for easy integration): 

* [gdx-pay.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay/0.10.2/gdx-pay-0.10.2-library.jar)
* [gdx-pay-server.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-server/0.10.2/gdx-pay-server-0.10.2-library.jar)

How to integrate in your server: 
```
 // create a manager which returns "true" by default  
 PurchaseVerifierManager verifier = new PurchaseVerifierManager(true);
 
 // add the various purchase verifiers
 verifier.addVerifier(new PurchaseVerifierAndroidGoogle(...));
 verifier.addVerifier(new PurchaseVerifierAndroidAmazon(...));
 verifier.addVerifier(new PurchaseVerifierAndroidOUYA(...));
 verifier.addVerifier(new PurchaseVerifieriOSApple(...));
 ...
 
 // verify a purchase
 if (verifier.isValid(transaction)) {
   // transaction appears valid
   ... add to BD etc. ...
 }
 else {
   // transaction appears bogus
   ... punish user ...
 }
 ```
 
Please note that the server-side functionality hasn't been fully developed yet as of this writing. 
**PurchaseVerifieriOSApple** is somewhat rudimentary implemented and will need some more work. There is a main(...) 
method in that fail if you want to run/test it :)

### Release History

Release history for major milestones (available via Maven):
* (in progress) 0.10.3: Bugfix release for issue #118 #120 #122 
* Version 0.10.2: Bugfix release for issue #117
* Version 0.10.1: Bugfix release for issue #113
* Version 0.10.0: Bugfix release for issue #110
* Version 0.9.2: Bugfix release for issue #91
* Version 0.9.1: Bugfix release for issue #88
* Version 0.9.0: New [Google Play Store implementation](gdx-pay-android-googleplay/README.md), no longer depends on OpenIAB (2016-02-06)
* Version 0.8.0: bugfix release (2015-12-16)
* Version 0.7.0: lastest updates/bugfixes integrated (2015-12-10)
* Version 0.6.0: iOS 9 support, bug fixes
* Version 0.5.0: Bugfixes for OpenIAB/Apple backends especially (2015-03-18)
* Version 0.3.0: Minor Updates and Bugfixes (2015-01-27)
* Version 0.2.0: iOS Added
* Version 0.1.0: Initial Release

### News & Community

Check the [libGDX blog](http://www.badlogicgames.com/) for news and updates.
You can get help on the [libGDX forum](http://www.badlogicgames.com/forum/) and talk to other users on the IRC channel #libgdx at irc.freenode.net.

### Reporting Issues

Something not working quite as expected? Do you need a feature that has not been implemented yet? Check the [issue tracker](https://github.com/libgdx/gdx-pay/issues) and add a new one if your problem is not already listed. Please try to provide a detailed description of your problem, including the steps to reproduce it.

### Using gdx-pay locally build binaries in a project

When Gradle is used to manage dependencies, gdx-pay locally build SNAPSHOT binaries can be used via the Maven local repository.

#### Install gdx-pay binaries in local Maven repository

Build gdx-pay with the following command:
`./gradlew assemble uploadArchives -PLOCAL`

#### Use Maven local SNAPSHOT version in a project 

1: add `mavenLocal()` as Gradle repository in the root `build.gradle`

    allprojects {
        repositories {
           mavenLocal()
        }
    }

2: change the version of gdx-pay dependencies to the `version` variable value found in gdx-pay/build.gradle

### Contributing

Awesome! If you would like to contribute with a new feature or a bugfix, [fork this repo](https://help.github.com/articles/fork-a-repo) and [submit a pull request](https://help.github.com/articles/using-pull-requests).
Also, before we can accept substantial code contributions, we need you to sign the [libGDX Contributor License Agreement](https://github.com/libgdx/libgdx/wiki/Contributing#contributor-license-agreement).

### License

The gdx-pay project is licensed under the [Apache 2 License](https://github.com/libgdx/gdx-pay/blob/master/LICENSE), meaning you can use it free of charge, without strings attached in commercial and non-commercial projects. We love to get (non-mandatory) credit in case you release a game or app using gdx-pay!

