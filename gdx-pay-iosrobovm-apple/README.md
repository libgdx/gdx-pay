# InApp purchasing implementation for Apple (iOS/RoboVM)

### Dependencies

     compile "com.badlogicgames.gdxpay:gdx-pay-iosrobovm-apple:$gdxPayVersion"

### Instantiation

Add this to your `IOSLauncher`:

    game.purchaseManager = new PurchaseManageriOSApple();

## Testing
Next to other ways, I find the easiest way to test the IAP the following: 

* draft your IAP in AppStore Connect<sup>1</sup>
* upload your build with IAPs to AppStore connect
* release your build with TestFlight for internal or external test users
* your build installed from TestFlight will have working IAPs that are not charged to the users


(1) In order for you  to use your actual IAP in your app you also have to fill some information regarding your App Store 
Connect account. Normally you will see a warning if something is missing, but sometimes when your app is marked as 
distributed for free, the warnings won't show. In case of IAP, make sure that you have filled required information in 
following section: My Apps -> Agreements, Tax, and Banking -> Paid Apps. It's status should be "Active". As stated 
there: "The Paid Apps agreement alllows your organization to sell apps on the App Store or **offer in-app purchases.**"


## Subscriptions

To verify if the user has a valid subscription we recommend server-side validation.

If you do not want to user server-side validation, it can be done by parsing receipt in the App. GdxPay has not 
implemented that. Pull requests are welcome :).

It is still possible to find out if user has a valid subscription.

iOS keeps expired Transactions from subscriptions in it's SkPaymentTransaction queues (as apposed to Google Play, which 
does not return them in the list of purchases).

All Transactions, including historical transactions, are passed through to the `PurchaseObserver`.

For example, if a user has a subscription with monthly period going on for 6 months and restores purchases, 
6 Transactions will be passed too in PurchaseObserver#handleRestore().

Filter out expired purchases manually. Some pointers:

* Start time of Transaction: `Transaction#getPurchaseTime()`  
* Reference to Product Information: `Transaction#getInformation()`
* Reference to Subscription period: `Information#getSubscriptionPeriod()`

Putting that together, you can calculate Transaction purchaseEndTime. 

If you have Billing Grace Period enabled in App Store Connect, you should add those days to the purchaseEndTime.

This logic is covered by `Transaction#calculateSubscriptionEndDate(int billingGracePeriodInDays)`

If there are zero transactions with your calculated purchaseEndTime available, the user has cancelled his subscription 
and should resubscribe. 

Limitations of this method:

* payment cancellations cannot be detected
* free trial periods cannot be detected; if someone decides to start a free trial of 3 days of a one-year subscription,
  the user will get the full year for free if he cancels after the first day.
 