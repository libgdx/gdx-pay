# InApp purchasing implementation for Google Play Billing

Handles purchases and restores for non-consumable and consumable products.

Subscriptions are supported with some limitations: the first `SubscriptionOfferDetails` in the list of offers in `ProductDetails` is used.

## Usage

### Dependencies

*android:*

     compile "com.badlogicgames.gdxpay:gdx-pay-android-googlebilling:$gdxPayVersion"


### ProGuard configuration
Just one line:

      -keep class com.android.vending.billing.**

### Instantiation

Add this to your `AndroidLauncher`'s `onCreate` method:

    game.purchaseManager = new PurchaseManagerGoogleBilling(this);

## Purchase items

Google Billing allows multiple products in the same purchase. You can change this setting at Play Console - In App Products - Product Setting - Allow Users to purchase multiple products in one purchase. This library forces handling only one product per transaction, so keep this setting "FALSE", or change as you need.


## Testing
* Upload your compile build with this lib included as an closed alpha build first. This is needed to even being able to add IAPs in the console.
* Add your IAPs and mark them as active
* Add testers to your closed alpha build so they see the new IAPs and also add these testers as "licensed testers" (Settings -> Account details -> Licensed) so they are not being charged
* Testers must join the alpha test channel and can test (in my experiences they can also test with a debug build)
