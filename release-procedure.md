# Steps to perform a release

## First release

You need a Nexus OSS account to publish the release with access to gdx-pay. Get in touch with one of the other maintainers to get one.

## Every release

1. Draft a new release from https://github.com/libgdx/gdx-pay/releases

2. Wait until the GitHub action completes.

3. Go to https://oss.sonatype.org --> "Staging Repositories"
   - wait until gdx-pay release appears here.
   - select "Close", then "Release" to fully release (needs signing!)

4. Update gdx-pay README.md --> search & replace version numbers!
   update/tag git: e.g. v1.3.4

5. Update gdx-pay gradle.properties
   update version to the next development version.
