# In-app purchasing implementation for Apple (iOS/MOE)

This module uses Apple's StoreKit 2 API through a small Swift bridge. The Java `PurchaseManageriOSApple`
implementation talks to the bridge through MOE/NatJ-compatible Objective-C selectors instead of depending on
RoboVM StoreKit bindings.

### Dependencies

     implementation "com.badlogicgames.gdxpay:gdx-pay-iosmoe-apple:$gdxPayVersion"

### Native StoreKit 2 bridge

The Swift bridge lives in:

     native/GdxPayStoreKit2Bridge

Build it as an iOS framework or XCFramework and include it in the MOE iOS application target. The exported Swift class
is `GDXStoreKit2Bridge`; the Java binding is mirrored in:

     src/main/java/com/badlogic/gdx/pay/ios/apple/bindings/GDXStoreKit2Bridge.java

If the Swift API changes, regenerate or update this NatJ binding so the selectors stay aligned:

* `shared`
* `canMakePayments`
* `fetchProductsWithIdentifiers:completion:`
* `purchaseWithIdentifier:completion:`
* `fetchCurrentEntitlementsWithCompletion:`
* `restorePurchasesWithCompletion:`
* `startObservingTransactionsWithCompletion:`
* `stopObservingTransactions`

### Building and publishing locally

In Android Studio, use JDK 17 or newer as the Gradle JVM. The module uses a Java 17 toolchain to compile Java 11
bytecode, matching the rest of this repository.

Useful Gradle tasks:

     ./gradlew :gdx-pay-iosmoe-apple:build
     ./gradlew :gdx-pay-iosmoe-apple:publishToMavenLocal

### Instantiation

Add this to your `IOSLauncher`:

    game.purchaseManager = new PurchaseManageriOSApple();

## Testing
Next to other ways, I find the easiest way to test the IAP the following:

* draft your IAP in AppStore Connect<sup>1</sup>
* upload your build with IAPs to AppStore connect
* release your build with TestFlight for internal or external test users
* your build installed from TestFlight will have working IAPs that are not charged to the users


(1) In order for you  to use your actual IAP in your app you also have to fill some information regarding your App Store Connect account. Normally you will see a warning if something is missing, but sometimes when your app is marked as distributed for free, the warnings won't show. In case of IAP, make sure that you have filled required information in following section:
My Apps -> Agreements, Tax, and Banking -> Paid Apps. It's status should be "Active". As stated there: "The Paid Apps agreement alllows your organization to sell apps on the App Store or **offer in-app purchases.**"
