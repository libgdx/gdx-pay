# Android Google Play implementation status

## Milestone 1

* DONE: implement install() and handleInstall()
* DONE: implement getInformation()
* DONE: implement dispose()
* DONE: handle restore()
* DONE: Check Android app lifecycle, do we need specific code for cornercases?
* DONE: Fail fast for other product types than ENTTITLEMENT type.
* DONE: Refactor and cleanup.
* DONE: IN PROGRESSS Integrate publish.gradle to android project to be able to publish to a maven repo.
* DONE: test SNAPSHOT version
* TODO:Describe com.badlogic.gdx.pay.PurchaseSystem#onAppRestarted on gdx-pay README
* release new version.

## Milestone 2
* Do agreed naming refactorings
* Verify purchase cancellation.
* Verify purchase authenticity
* Handle purchase non-consumable product
* Support working with AndroidFragmentApplication
* Hande purchase consumable product
* Handle purchase subscription
* Set up CI (e.g. travis ci)
* update root README.md (update versions)
* integrate publish.gradle (support android library project).
* Check for TODO's
* Move testdata (objectmothers) to a common place (gdx-pay-testdata?)


## Refactorings to be discussed:

* Rename Information to Product (fits its name better)
* Should we rename Transaction to Purchase? At least matches better Google Play store naming.
* Rename com.badlogic.gdx.pay.Transaction.getIdentifier() to com.badlogic.gdx.pay.Transaction.getProductId();
* Rename gdx-pay submodule to gdx-pay-api
* create a -client and -server submodule
* gdx-pay-client: move classes in this project to com.badlogic.gdx.pay.client
* introduce gdx-pay-client-testdata module, put here in all ObjectMother classes.
