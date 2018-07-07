# InApp purchasing implementation for Google Play

Handles purchases and restores for non-consumable and consumable products, will not work as intended for subscriptions.

## Why yet another implementation?

* OpenIAB is currently unmaintained and has open issues, making payments fail too often
* gdx-pay-android-googleplay uses Google's old AIDL-based approach while this implementation uses the convinient Google Play Billing Library which handles all the complicated stuff.
* No need to change your AndroidManifest
* No unneeded dependencies included - your app will grow just about 30KB with this lib!

Hint: Probably you have to add

         maven { url "https://jcenter.bintray.com" }

to your Gradle file's repository paragraph.

## Proguard Config
Just one line:

      -keep class com.android.vending.billing.**
