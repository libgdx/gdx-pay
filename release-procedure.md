# Steps to perform a release

## First release

You need a Nexus OSS account to publish the release with access to gdx-pay. Get in touch with one of the other maintainers to get one.

## Every release

1. Make sure NEXUS_ secrets in GitHub Security are from the person publishing the release

2. Draft a new release from https://github.com/libgdx/gdx-pay/releases

3. Wait until the GitHub action completes.

4. Go to https://central.sonatype.com/publishing and log in, publish the release there.

5. Update gdx-pay README.md: search & replace version numbers

6. Update gdx-pay gradle.properties
   update version to the next development version.
