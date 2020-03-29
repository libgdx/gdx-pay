# InApp purchasing implementation for Apple (iOS/RoboVM)

### Dependencies

    api "com.badlogicgames.gdxpay:gdx-pay-iosrobovm-apple:$gdxPayVersion"

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
