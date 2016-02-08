# InApp purchasing implementation for Google Play

Handles purchases and restores for non-consumable products.

## Unique selling points

* Does not use OpenIAB (OpenIAB is currently unmaintained and has open issues, making payments fail too often)
* Detailed error reports: all exception messages contain as many details as possible in erroneous situations
* High quality: developed using TDD practices, aiming to deliver a high quality Android payment module.

## Limitations

It implements only features the users have needed so far. The following features are not yet implemented:

* No support for more than 700 purchases per user per app: we do not yet support INAPP_CONTINUATION_TOKEN	
* No support for consumables and subscriptions (only supports non-consumable products currently).
* No support for Libgdx applications that use AndroidFragmentApplication (only activity AndroidApplication is supported yet).

More details on [Status page](STATUS.md) 

## Contributing

We welcome contributions (bugfixes and new features).

For bugfixes, provide a unit-test that reproduces the bug and verifies it is fixed.

For new features, please provide unit tests for all scenarios that might occur.