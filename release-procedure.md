# Steps to perform a release

## First release

You need a Nexus OSS account and a GPG key in order to generate required `.asc` files.

In ~/.gradle/gradle.properties, define the following properties:

NEXUS_USERNAME=(username)
NEXUS_PASSWORD=(password)
signing.keyId=(gpg1 key id, or last 8 chars of gpg2 key)
signing.password=gpg password
signing.secretKeyRingFile=/Users/username/.gnupg/secring.gpg

Correct the properties. Create a gpg key if you have not yet one.

## Every release

1. edit project/build.gradle version
   remove "-SNAPSHOT" from version.

2. assemble local build with correct version as last final check:
    cd to gdx-pay and run: "./gradlew clean && ./gradlew build"

3. upload archives via console:
    cd to gdx-pay and run: "./gradlew -P RELEASE uploadArchives"

4. for RELEASE go to https://oss.sonatype.org --> "Staging Repositories"
   - select "Close", then "Release" to fully release (needs signing!)
   - signing: needs that sonatype key from above...

5. update e.g. gdx-pay README.md --> search & replace version numbers!
   update/tag git: e.g. v0.7.0
   update version to e.g. 0.8.0-SNAPSHOT in build.gradle

