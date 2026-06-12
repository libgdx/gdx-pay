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

### Note for Kotlin users

Both `PurchaseManageriOSApple` and `PurchaseManageriOSApple2` work with Java and Kotlin without any special configuration. All async operations and callbacks are handled internally by the `PurchaseManager` implementation — you interact with it through the standard `PurchaseObserver` interface regardless of your language.

Separately, there is also a Kotlin coroutine wrapper available (`com.mobidevelop.robovm:robopods-swift-storekit2-kt`) for projects that want to use the StoreKit 2 API directly without gdx-pay. This is **not** needed when using gdx-pay.

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

## Server-Side Purchase Validation

When validating purchases on your server, the approach differs between StoreKit 1 and StoreKit 2.

The `Transaction.getTransactionDataSignature()` field holds the data you need to validate. You can tell
which format you received: if the string contains dots (`.`), it is a JWS token (StoreKit 2); otherwise
it is a Base64-encoded receipt (StoreKit 1).

### StoreKit 1 — receipt validation (legacy)

`PurchaseManageriOSApple` sets `transactionDataSignature` to a **Base64-encoded receipt** string.

To validate it server-side, POST the receipt to Apple's `/verifyReceipt` endpoint:

```
POST https://buy.itunes.apple.com/verifyReceipt          (production)
POST https://sandbox.itunes.apple.com/verifyReceipt      (sandbox)

Body: { "receipt-data": "<base64-encoded receipt>" }
```

The gdx-pay server module ships a ready-made helper for this:
```java
PurchaseVerifieriOSApple verifier = new PurchaseVerifieriOSApple(false /* sandbox */);
boolean valid = verifier.isValid(transaction);
```

> **Note:** Apple's `/verifyReceipt` endpoint is considered legacy and may be deprecated in the future.
> New apps should prefer StoreKit 2 and JWS-based validation.

### StoreKit 2 — JWS validation (recommended)

`PurchaseManageriOSApple2` sets `transactionDataSignature` to a **JWS (JSON Web Signature)** string
obtained from `VerificationResult.getJwsRepresentation()`. This is a signed JWT issued directly by
Apple and does **not** require a network call back to Apple for validation — you verify the signature
locally using Apple's root certificate.

Apple provides an official Java library for this:
[apple/app-store-server-library-java](https://github.com/apple/app-store-server-library-java)

Example server-side validation with that library:

```java
// Read your signed transaction from the gdx-pay Transaction object
String jwsToken = transaction.getTransactionDataSignature();

// Set up the verifier (once, reuse it)
AppStoreServerAPIClient client = /* ... */;
SignedDataVerifier verifier = new SignedDataVerifier(
    rootCertificates,
    bundleId,
    bundleAppleId,
    Environment.PRODUCTION
);

JWSTransactionDecodedPayload payload = verifier.verifyAndDecodeTransaction(jwsToken);
// payload now contains the verified purchase details
```

### Distinguishing StoreKit 1 and StoreKit 2 receipts

If you need to handle both receipt formats in the same server code (for example, during a migration
from StoreKit 1 to StoreKit 2), you can detect the format by checking for a dot character:

```java
String sig = transaction.getTransactionDataSignature();
if (sig != null && sig.contains(".")) {
    // StoreKit 2: JWS token — validate with app-store-server-library-java
} else {
    // StoreKit 1: Base64 receipt — validate with /verifyReceipt endpoint
}
```

> **Migration note:** Switching from `PurchaseManageriOSApple` to `PurchaseManageriOSApple2` is a
> **breaking change** for any existing server-side validation code, because the format of
> `transactionDataSignature` changes from a Base64 receipt to a JWS token.

## Testing
Next to other ways, I find the easiest way to test the IAP the following: 

* draft your IAP in AppStore Connect<sup>1</sup>
* upload your build with IAPs to AppStore connect
* release your build with TestFlight for internal or external test users
* your build installed from TestFlight will have working IAPs that are not charged to the users


(1) In order for you  to use your actual IAP in your app you also have to fill some information regarding your App Store Connect account. Normally you will see a warning if something is missing, but sometimes when your app is marked as distributed for free, the warnings won't show. In case of IAP, make sure that you have filled required information in following section:
My Apps -> Agreements, Tax, and Banking -> Paid Apps. It's status should be "Active". As stated there: "The Paid Apps agreement alllows your organization to sell apps on the App Store or **offer in-app purchases.**"