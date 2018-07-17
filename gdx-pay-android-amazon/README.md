# InApp purchasing implementation for Amazon

Handles purchases and restores for non-consumable and consumable products, will not work as intended for subscriptions.

## Configuration

### Dependencies

 * android:

    compile "com.badlogicgames.gdxpay:gdx-pay-android-amazon:$gdxPayVersion"


### ProGuard configuration

     #IAP
     -dontwarn com.amazon.**
     -keep class com.amazon.** {*;}
     -keepattributes *Annotation*
     -optimizations !code/allocation/variable

## Testing
* Draft some IAPs in Amazon's developer console
* Export them as JSON and copy the file to your Amazon device
* Download App Tester vom Amazon App Store
* Use your debug build to test
* When done, compile your release build and submit it for live app testing
* Submit your IAPs
* Your live app testers can test the "real" behaviour without being charged