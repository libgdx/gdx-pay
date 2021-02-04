# InApp purchasing implementation for Google Play Billing

Handles purchases and restores for non-consumable and consumable products, subscriptions support is work in progress and PRs are welcome.

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

## Testing
* Upload your compile build with this lib included as an closed alpha build first. This is needed to even being able to add IAPs in the console.
* Add your IAPs and mark them as active
* Add testers to your closed alpha build so they see the new IAPs and also add these testers as "licensed testers" (Settings -> Account details -> Licsensed) so they are not being charged
* Testers must join the alpha test channel and can test (in my experiences they can also test with a debug build)
