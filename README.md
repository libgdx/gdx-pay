This project aims to provide a **cross-platform API for InApp purchasing**.
The gdx-pay project is a libGDX extension. Current release version is 0.9.0 (0.10.0 snapshots available
via sonatype). Please upgrade to the latest libGDX v1.9.1 and robovm v1.13.0.

### Getting Started

The purchasing API comes in two parts.

* **Client-Side API**: This is what is integrated into your game or application and will handle the
purchase flow for the application.

* **Server-Side API (optional)**: If you have a server running and would like to do a purchase verification
on your server, you can use the API to verify purchases sent to your server. 

To integrate using Maven, have a look at the [Wiki page for Maven Integration](https://github.com/libgdx/gdx-pay/wiki/Maven-Integration). In
that case you won't need to add the jars listed below.

#### Client-Side API

To setup the purchasing API, you will need to add the corresponding jar files to your project. In 
your **core project** you have:
* [gdx-pay.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay/0.9.0/gdx-pay-0.9.0-library.jar)
* [gdx-pay-client.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-client/0.9.0/gdx-pay-client-0.9.0-library.jar) 

In your **Android project** you use. Please note if you use the jars, all dependencies such as the ouya-sdk.jar are already wired in:
* [gdx-pay-android.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-android/0.9.0/gdx-pay-android-0.9.0-library.jar)
* [gdx-pay-android-googleplay.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-android-googleplay/0.9.0/gdx-pay-android-googleplay-0.9.0.jar) ( for Google Play with non-consumable products only, exclude this artifact when using other product types. [See status](gdx-pay-android-googleplay/README.md))
* [gdx-pay-android-openiab.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-android-openiab/0.9.0/gdx-pay-android-openiab-0.9.0-library.jar) (to support GooglePlay, Amazon etc. [This component is deprecated!](gdx-pay-android-openiab/README.md))
* [gdx-pay-android-ouya.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-android-ouya/0.9.0/gdx-pay-android-ouya-0.9.0-library.jar) (to support OUYA)
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
* [gdx-pay-iosrobovm-apple.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-iosrobovm-apple/0.9.0/gdx-pay-iosrobovm-apple-0.9.0-library.jar)
* robovm.xml: add `<pattern>com.badlogic.gdx.pay.ios.apple.PurchaseManageriOSApple</pattern>` inside `<forceLinkClasses>...</forceLinkClasses>`.

In your **Desktop project** you use:
* [gdx-pay-desktop-apple.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-desktop-apple/0.9.0/gdx-pay-desktop-apple-0.9.0-library.jar) (to support the Mac App Store): *needs implementation/volunteers wanted!*

In your **GWT project** you use:
* [gdx-pay-gwt-googlewallet.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-gwt-googlewallet/0.9.0/gdx-pay-gwt-googlewallet-0.9.0-library.jar) (to support Google Wallet): *needs implementation/volunteers wanted!*

In any case, if the correct jar files are place, all you need is to initialize the purchase system in your 
core project as follows without bothering making any code changes. 


```
...
// Disposes static instances in case JVM is re-used on restarts
PurchaseSystem.onAppRestarted();

if (PurchaseSystem.hasManager()) {
    

  // purchase system is ready to start. Let's initialize our product list etc...
  PurchaseManagerConfig config = new PurchaseManagerConfig();
  config.addOffer(...)
  config.addOffer(...)
  ...
  config.addStoreParam(PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE, "<Google key>");
  ...
  config.addStoreParam(PurchaseManagerConfig.STORE_NAME_ANDROID_OUYA, new Object[] { 
    OUYA_DEVELOPER_ID, 
    KEYPATH 
  });
  ...

  // let's start the purchase system...
  PurchaseSystem.install(new PurchaseObserver() {         
   ...
  }
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

`(*)` IMPORTANT: `PurchaseSystem.restore()` should *not be called directly* by your application. Restoring purchases shall only be 
called when a user explicitly requests it. In your application add a [Restore Purchases] button which in turn will call this method.
This is a requirement by Apple iOS. If you don't provide a button for purchase restores your application will be rejected! You have
been warned :)

For keys you have to pass in have a look at [OpenIAB's Sample Configuration](https://github.com/onepf/OpenIAB/blob/dev/samples/trivialdrive/src/main/java/org/onepf/sample/trivialdrive/InAppConfig.java).
Please note you will not need to pass in any keys for Amazon as it doesn't require them.

Here is a user-guide in the wiki on how to implement gdx-pay in your libgdx project with using platform resolvers: 
 [Integration-example-with-resolvers](https://github.com/libgdx/gdx-pay/wiki/Integration-example-with-resolvers)


#### Server-Side API (Optional)

**gdx-pay-server** is optional and can be used for purchase **verification on a server**. It verifies if a 
purchase is valid by e.g. doing a post-back validation on a server. 
Place the following two jar-files onto your server (you won't need any other libGDX 
libraries on your server, all dependencies to libGDX have been removed for easy integration): 

* [gdx-pay.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay/0.9.0/gdx-pay-0.9.0-library.jar) 
* [gdx-pay-server.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-server/0.9.0/gdx-pay-server-0.9.0-library.jar) 

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

### Contributing

Awesome! If you would like to contribute with a new feature or a bugfix, [fork this repo](https://help.github.com/articles/fork-a-repo) and [submit a pull request](https://help.github.com/articles/using-pull-requests).
Also, before we can accept substantial code contributions, we need you to sign the [libGDX Contributor License Agreement](https://github.com/libgdx/libgdx/wiki/Contributing#contributor-license-agreement).

### License

The gdx-pay project is licensed under the [Apache 2 License](https://github.com/libgdx/gdx-pay/blob/master/LICENSE), meaning you can use it free of charge, without strings attached in commercial and non-commercial projects. We love to get (non-mandatory) credit in case you release a game or app using gdx-pay!

