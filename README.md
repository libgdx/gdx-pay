[![Build Status](https://travis-ci.org/libgdx/gdx-pay.svg?branch=master)](https://travis-ci.org/libgdx/gdx-pay)
[![Maven Central](http://maven-badges.herokuapp.com/maven-central/com.badlogicgames.gdxpay/gdx-pay/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.badlogicgames.gdxpay%22)
[![Dependency Status](https://dependencyci.com/github/libgdx/gdx-pay/badge)](https://dependencyci.com/github/libgdx/gdx-pay)

This project aims to provide a **cross-platform API for InApp purchasing**.
The gdx-pay project is a libGDX extension. Current release version is 1.3.3. Please use at least libGDX v1.9.8 or Robovm 2.3.5.

SNAPSHOT builds are published regularly on [https://oss.sonatype.org/content/repositories/snapshots/](https://oss.sonatype.org/content/repositories/snapshots/).

### Supported  payment services

* **Google Play (Android)**: [googlebilling](https://github.com/libgdx/gdx-pay/tree/master/gdx-pay-android-googlebilling)

* **Amazon IAP (Android)**: [amazon](https://github.com/libgdx/gdx-pay/tree/master/gdx-pay-android-amazon)

* **Huawei (Android)**: [huawei](https://github.com/libgdx/gdx-pay/tree/master/gdx-pay-android-huawei)

* **Apple (iOS)**: [RoboVM](https://github.com/libgdx/gdx-pay/tree/master/gdx-pay-iosrobovm-apple)

Click on the links to view the subproject's readme files for service-dependant information and artifacts.

### Installation

The recommended way to use gdx-pay is via dependency management with Gradle or Maven. Artifacts are available in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.badlogicgames.gdxpay%22).

*project-root/build.gradle:*

    ext {
        gdxPayVersion = '1.3.3'
    }

Add the following dependencies:

*core:*

        compile "com.badlogicgames.gdxpay:gdx-pay-client:$gdxPayVersion"

*html:*

        compile "com.badlogicgames.gdxpay:gdx-pay:$gdxPayVersion:sources"
        compile "com.badlogicgames.gdxpay:gdx-pay-client:$gdxPayVersion:sources"

You also need to add the following file to your GdxDefinition.gwt.xml in your html project:

	    <inherits name="com.badlogic.gdx.pay_client"/>

That's all you need to use gdx-pay in the core project. Of course, you want to use a certain IAP service in your game.
Look in the service subproject's readme files linked above.
    
### Usage

The main interface you use to communicate with payment services is the `PurchaseManager`. Add a field holding it 
to your main game class:

    public PurchaseManager purchaseManager;

In the launcher class you instantiate the PurchaseManager for the payment service you want to use:

    game.purchaseManager = new DesiredPlatformPurchaseManager(...);

See the documentation of your desired payment service linked above on how to instantiate its `PurchaseManager` implementation.

#### Configuration

Before using the PurchaseManager for payments, it needs to get installed:
You need to provide a callback listener implementing the `PurchaseObserver` interface and a configuration. 
Typically, the configuration just passes the items you want to offer: 

    PurchaseManagerConfig pmc = new PurchaseManagerConfig();
    pmc.addOffer(new Offer().setType(OfferType.ENTITLEMENT).setIdentifier(YOUR_ITEM_SKU));
    pmc.addOffer(new Offer().setType(OfferType.CONSUMABLE).setIdentifier(YOUR_ITEM_SKU));
    pmc.addOffer(new Offer().setType(OfferType.SUBSCRIPTION).setIdentifier(YOUR_ITEM_SKU));
    // some payment services might need special parameters, see documentation
    pmc.addStoreParam(storename, param)
    
    purchaseManager.install(new MyPurchaseObserver(), pmc, true);

When the PurchaseManager is sucessfully installed, your `PurchaseObserver` will receive a
 callback and `purchaseManager.installed()` will return `true`. That might take some seconds depending 
 on the payment service. You can now request information or purchase items.
 
If you are completely done with the `PurchaseManager`, call its `dispose()` method.
 
#### Request item information

It is important to know which of the items you added to the configuration are available at which
price. Use `getInformation()` to retrieve an item `Information` object to do so:

    Information skuInfo = purchaseManager.getInformation(sku);
    if (skuInfo == null || skuInfo.equals(Information.UNAVAILABLE)) {
       // the item is not available...
       purchaseButton.setDisabled(true);
    } else {
       // enable a purchase button and set its price label
       purchaseButton.setText(skuInfo.getLocalPricing());
    }
        
#### Purchase items

This is for what you are reading this! It is pretty easy to start a purchasement:

    purchaseManager.purchase(sku);
    
If the purchasement was successfully done, 
you will receive a call to `PurchaseObserver.handlePurchase()`. If there was an error, 
you *might* receive a call to your observer's `handlePurchaseError()` or `handlePurchaseCanceled()` 
method.

#### Restore purchases

If the user reinstalls your game or erased its data, it is important to let him restore his past purchases.
You can do so by calling

    purchaseManager.purchaseRestore()
    
You will get a callback to your observer's `handleRestore()` method with a list of past transactions.

**Please note:** Don't use this to query the user's bought entitlements on every game start,
but persist them yourself. Call this method only when the user hits a "reclaim" button. The most important reasons 
for this:

 * (iOS only) Apple will reject your game if it calls `purchaseRestore()` without user interaction
 * You get only reliable results if the device is connected to the internet. If you don't persist
  entitlements yourself, your paying users are not able to use their purchases offline.
 * `purchaseRestore()` might take some time to fetch its results 

### Example project

If you have questions or problems, take a look at the [example project](https://github.com/libgdx/gdx-pay-example) 
demonstrating how to configure and use gdx-pay. 

### News & Community

Check the [libGDX blog](http://www.badlogicgames.com/) for news and updates.
You can get help on the [libGDX forum](http://www.badlogicgames.com/forum/) and talk to other users on the 
IRC channel #libgdx at irc.freenode.net or the 
[libgdx discord](https://discord.gg/6pgDK9F).

### Reporting Issues

Something not working quite as expected? Do you need a feature that has not been implemented yet? Check the [issue tracker](https://github.com/libgdx/gdx-pay/issues) and add a new one if your problem is not already listed. Please try to provide a detailed description of your problem, including the steps to reproduce it.

### Building from source

To build from source, clone or download this repository, then open it in Android Studio. Perform the following command to compile and upload the library in your local repository:

    ./gradlew publishToMavenLocal

See build.gradle file for current version to use in your dependencies.

### Contributing

Awesome! If you would like to contribute with a new feature or a bugfix, [fork this repo](https://help.github.com/articles/fork-a-repo) and [submit a pull request](https://help.github.com/articles/using-pull-requests).
Also, before we can accept substantial code contributions, we need you to sign the [libGDX Contributor License Agreement](https://github.com/libgdx/libgdx/wiki/Contributing#contributor-license-agreement).

### License

The gdx-pay project is licensed under the [Apache 2 License](https://github.com/libgdx/gdx-pay/blob/master/LICENSE), meaning you can use it free of charge, without strings attached in commercial and non-commercial projects. We love to get (non-mandatory) credit in case you release a game or app using gdx-pay!

