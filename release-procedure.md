1. edit project/build.gradle version
   NOTE: make sure to remove "SNAPSHOT" when building otherwise
         references are broken!

2. upload archives via console:
      cd to gdx-pay and run: "./gradlew -P RELEASE uploadArchives"

3. for RELEASE go to https://oss.sonatype.org --> "Staging Repositories"
   - select "Close", then "Release" to fully release (needs signing!)
   - signing: needs that sonatype key from above...

4. update e.g. gdx-pay README.md --> search & replace version numbers!
   update/tag git: e.g. v0.7.0
   update version to e.g. 0.8.0-SNAPSHOT in build.gradle

