This project aims to provide a common API for InApp purchasing across supported platforms.
The gdx-pay project is a libGDX extension.

### Getting Started

The purchasing API comes in two part.

* **Client-Side API**: This is what is integrated into your game or application and will handle the
purchase flow for the application.

* **Server-Side API (optional)**: If you have a server running and would like to do a purchase verification
on your server, you can use the API to verify purchases sent to your server. The server-side API is optional to use. 

#### Client-Side API

To setup the purchasing API, you will need to add the corresponding jar files to your project. In 
your **core project** you have:
* gdx-pay.jar (required)

In your **Android project** you use:
* gdx-pay-android.jar (required)
* gdx-pay-android-openiab.jar (optional: to support GooglePlay, Amazon etc.)
* gdx-pay-android-ouya.jar (optional: to support OUYA)

In your **iOS project** you use:
* gdx-pay-iosrobovm-apple.jar (required): *needs implementation/volunteers wanted*!

In your **Desktop project** you use:
* gdx-pay-desktop-apple.jar (optional: to support the Mac App Store): *needs implementation/volunteers wanted*!

In your **GWT project** you use:
* gdx-pay-gwt-googlewallet.jar (optional: to support Google Wallet): *needs implementation/volunteers wanted*!

Also, for Android you will need to (a) update your AndroidManifest.xml and (b) proguard.cfg.

In any case, if the correct jar files are place, all you need is to initialize the purchase system in your 
core project as follows without bothering making any code changes to Android (and hopefully Desktop/iOS later as well). 


```
...
if (PurchaseSystem.hasManager()) {
    // purchase system is ready to start. Let's initialize our product list etc...
    PurchaseManagerConfig config = new PurchaseManagerConfig();
    config.addOffer(...)
    config.addOffer(...)
    ...
    config.addStoreParam(PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE, "<Google key>");
    config.addStoreParam(PurchaseManagerConfig.STORE_NAME_ANDROID_AMAZON, "<Amazon key>");
    ...
    config.addStoreParam(PurchaseManagerConfig.STORE_NAME_ANDROID_OUYA, new Object[] {
      "<OUYA developerID String",
      new byte[] { <OUYA applicationKey> }
    });
    ...

    // let's start the purchase system...
    PurchaseSystem.install(new PurchaseObserver() {         
     ...
    }
    ...
    // to restore existing purchases (results are reported to the observer)
    PurchaseSystem.restore();
    ...
    // to make a purchase (results are reported to the observer)
    PurchaseSystem.purchase("product_identifier"); 
    ...
}
...
```

#### Server-Side API (Optional)

**gdx-pay-server** is optional and can be used for purchase **verification on a server**. It verifies if a 
purchase is valid by e.g. doing a post-back validation on a server. 
Place the following two jar-files onto your server (you won't need any other libGDX 
libraries on your server, all dependencies to libGDX have been removed for easy integration): 

* gdx-pay.jar 
* gdx-pay-server.jar 

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

