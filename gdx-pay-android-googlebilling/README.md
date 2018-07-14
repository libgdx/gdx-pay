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

## Testing
* Upload your compile build with this lib included as an closed alpha build first. This is needed to even being able to add IAPs in the console.
* Add your IAPs and mark them as active
* Add testers to your closed alpha build so they see the new IAPs and also add these testers as "licensed testers" (Settings -> Account details -> Licsensed) so they are not being charged
* Testers must join the alpha test channel and can test (in my experiences they can also test with a debug build)
* Important you need a second account because you cannot buy IAPs yourself
