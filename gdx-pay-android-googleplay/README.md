# Gdx-pay implementation for Android applications on Google Play.

Supports non-consumable products products only currently.

## Unique selling points

* Does not depend on OpenIAB (OpenIAB gives us many errors and issues)
* Well-tested with Junit
* All exception messages contain as many details as possible to understand whats going on. 

## Known issues and limitations

* No support for more than 700 purchases per user per app: we do not yet support INAPP_CONTINUATION_TOKEN	
* No support for consumables and subscriptions (only supports non-consumable products currently).
* No support for Libgdx applications that use AndroidFragmentApplication (only activity AndroidApplication is supported yet).
