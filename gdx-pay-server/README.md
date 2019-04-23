# Server-Side API (Optional)

**gdx-pay-server** is optional and can be used for purchase **verification on a server**. It verifies if a
purchase is valid by e.g. doing a post-back validation on a server.
Place the following two jar-files onto your server (you won't need any other libGDX
libraries on your server, all dependencies to libGDX have been removed for easy integration):

* [gdx-pay.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay/0.12.1/gdx-pay-0.12.1-library.jar)
* [gdx-pay-server.jar](https://oss.sonatype.org/content/repositories/releases/com/badlogicgames/gdxpay/gdx-pay-server/0.12.1/gdx-pay-server-0.12.1-library.jar)

How to integrate in your server:
```
 // create a manager which returns "true" by default
 PurchaseVerifierManager verifier = new PurchaseVerifierManager(true);

 // add the various purchase verifiers
 verifier.addVerifier(new PurchaseVerifierAndroidGoogle(...));
 verifier.addVerifier(new PurchaseVerifierAndroidAmazon(...));
 verifier.addVerifier(new PurchaseVerifieriOSApple(...));
 ...

 // verify a purchase
 if (verifier.isValid(transaction)) {
   // transaction appears valid
   ... add to BD etc. ...
 }
 else {
   // transaction appears bogus
   ... punish user ...
 }
 ```

Please note that the server-side functionality hasn't been fully developed yet as of this writing.
**PurchaseVerifieriOSApple** is somewhat rudimentary implemented and will need some more work. There is a main(...)
method in that fail if you want to run/test it :)

