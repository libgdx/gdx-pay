# InApp purchasing implementation for Apple (iOS/RoboVM)

This module provides two `PurchaseManager` implementations for iOS:

| Implementation | API | Minimum iOS version |
|---|---|---|
| `PurchaseManageriOSApple` | StoreKit 1 | iOS 7+ |
| `PurchaseManageriOSApple2` | StoreKit 2 | iOS 15+ |

## Choosing between StoreKit 1 and StoreKit 2

**StoreKit 2** (`PurchaseManageriOSApple2`) is the recommended implementation for new projects. It uses Apple's modern StoreKit 2 Swift-based API (via [RoboVM StoreKit 2 bindings](https://github.com/MobiVM/robovm-cocoatouch-swift)) and provides:

* Improved subscription handling, including eligibility checks for introductory offers via `isEligibleForIntroOffer()`
* A modern async-based API under the hood

**StoreKit 1** (`PurchaseManageriOSApple`) should be used if your app needs to support iOS versions below 15.

If your app targets a range of iOS versions, you can select the implementation at runtime based on the device's iOS version (see [Instantiation](#instantiation) below).

## Dependencies

     implementation "com.badlogicgames.gdxpay:gdx-pay-iosrobovm-apple:$gdxPayVersion"

This single dependency includes both `PurchaseManageriOSApple` (StoreKit 1) and `PurchaseManageriOSApple2` (StoreKit 2).

The StoreKit 2 implementation depends on the RoboVM StoreKit 2 bindings, which are included as a transitive dependency:

     com.mobidevelop.robovm:robopods-swift-storekit2

If your app only uses StoreKit 1 and you want to exclude the StoreKit 2 transitive dependency, you can do so in your Gradle configuration:

     implementation("com.badlogicgames.gdxpay:gdx-pay-iosrobovm-apple:$gdxPayVersion") {
         exclude group: 'com.mobidevelop.robovm', module: 'robopods-swift-storekit2'
     }

## Instantiation

### Using StoreKit 1 only

Add this to your `IOSLauncher`:

    game.purchaseManager = new PurchaseManageriOSApple();

### Using StoreKit 2 only

Add this to your `IOSLauncher`:

    game.purchaseManager = new PurchaseManageriOSApple2();

### Selecting at runtime based on iOS version

If your app supports both older and newer iOS versions, you can choose the implementation at runtime:

    import org.robovm.apple.foundation.Foundation;

    if (Foundation.getMajorSystemVersion() >= 15) {
        game.purchaseManager = new PurchaseManageriOSApple2();
    } else {
        game.purchaseManager = new PurchaseManageriOSApple();
    }

## Testing
Next to other ways, I find the easiest way to test the IAP the following: 

* draft your IAP in AppStore Connect<sup>1</sup>
* upload your build with IAPs to AppStore connect
* release your build with TestFlight for internal or external test users
* your build installed from TestFlight will have working IAPs that are not charged to the users


(1) In order for you  to use your actual IAP in your app you also have to fill some information regarding your App Store Connect account. Normally you will see a warning if something is missing, but sometimes when your app is marked as distributed for free, the warnings won't show. In case of IAP, make sure that you have filled required information in following section:
My Apps -> Agreements, Tax, and Banking -> Paid Apps. It's status should be "Active". As stated there: "The Paid Apps agreement alllows your organization to sell apps on the App Store or **offer in-app purchases.**"