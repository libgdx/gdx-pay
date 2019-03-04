# InApp purchasing implementation for Apple (iOS/RoboVM)

### Dependencies

     compile "com.badlogicgames.gdxpay:gdx-pay-iosrobovm-apple:$gdxPayVersion"

### Instantiation

Add this to your `IOSLauncher`:

    game.purchaseManager = new PurchaseManageriOSApple();

## Testing
Next to other ways, I find the easiest way to test the IAP the following: 

* draft your IAP in AppStore Connect
* upload your build with IAPs to AppStore connect
* release your build with TestFlight for internal or external test users
* your build installed from TestFlight will have working IAPs that are not charged to the users
