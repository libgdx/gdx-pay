# IAP implementation with HMS (Huawei or an android device with HMS app)

It manages purchases, restores and consumption for consumable, non-consumable and subscription products.

## Usage

### Dependencies

 *android:*

     compile "com.badlogicgames.gdxpay:gdx-pay-android-huawei:$gdxPayVersion"


### ProGuard configuration

     -ignorewarning
     -keepattributes *Annotation*
     -keepattributes Exceptions
     -keepattributes InnerClasses
     -keepattributes Signature
     -keepattributes SourceFile,LineNumberTable
     -keep class com.hianalytics.android.**{*;}
     -keep class com.huawei.updatesdk.**{*;}
     -keep class com.huawei.hms.**{*;}
     
### AndResGuard configuration

     "R.string.hms*",
     "R.string.connect_server_fail_prompt_toast",
     "R.string.getting_message_fail_prompt_toast",
     "R.string.no_available_network_prompt_toast",
     "R.string.third_app_*",
     "R.string.upsdk_*",
     "R.string.agc*",
     "R.layout.hms*",
     "R.layout.upsdk_*",
     "R.drawable.upsdk*",
     "R.color.upsdk*",
     "R.dimen.upsdk*",
     "R.style.upsdk*"

### REQUIREMENTS
* https://developer.huawei.com/consumer/en/codelab/HMSInAppPurchase/index.html#1
* https://consumer.huawei.com/ca/support/content/en-us00698213/

### PREPARATION
* https://developer.huawei.com/consumer/en/codelab/HMSInAppPurchase/index.html#2
* enabling IAP service : https://developer.huawei.com/consumer/en/codelab/HMSInAppPurchase/index.html#3
* configuring products: https://developer.huawei.com/consumer/en/codelab/HMSInAppPurchase/index.html#4
* integrate HMS sdk into your app: https://developer.huawei.com/consumer/en/codelab/HMSInAppPurchase/index.html#5

### How To use the HuaweiPurchaseManager

* Into your Activity (extending AndroidApplication), You have to instantiate the HuaweiPurchaseManager;
    `HuaweiPurchaseManager huaweiPurchaseManager = new HuaweiPurchaseManager(this);`
* Then, You need to set the HuaweiPurchaseManager as GdxGame's PurchaseManager:
    `game.purchaseManager = huaweiPurchaseManager;`

## Testing
* Sandbox testing: https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/iap-sandbox-testing-v4