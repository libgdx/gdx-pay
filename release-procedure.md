# Steps to perform a release

## First release

You need a Nexus OSS account to publish the release with access to gdx-pay. Get in touch with one of the other maintainers to get one.

## Every release

1. Draft a new release from https://github.com/libgdx/gdx-pay/releases

2. Wait until the GitHub action completes.

3. Go to https://central.sonatype.com/publishing and log in
   - wait until gdx-pay release appears here.
   - select "Close"
   - Approx 2 minutes later Release button becomes enabled, click "Release" and confirm.
   - In approx 30 minutes the release will be available in Maven Central.

   Note: I did not see the release appear here, but another maintainer did see it. 
   See https://github.com/libgdx/gdx-pay/issues/278 for the discussion

4. Update gdx-pay README.md: search & replace version numbers

5. Update gdx-pay gradle.properties
   update version to the next development version.
