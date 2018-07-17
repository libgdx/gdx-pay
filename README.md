[![Build Status](https://travis-ci.org/libgdx/gdx-pay.svg?branch=master)](https://travis-ci.org/libgdx/gdx-pay)
[![Maven Central](http://maven-badges.herokuapp.com/maven-central/com.badlogicgames.gdxpay/gdx-pay/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.badlogicgames.gdxpay%22)
[![Dependency Status](https://dependencyci.com/github/libgdx/gdx-pay/badge)](https://dependencyci.com/github/libgdx/gdx-pay)



This project aims to provide a **cross-platform API for InApp purchasing**.
The gdx-pay project is a libGDX extension. Current release version is 0.11.2. Please use at least libGDX v1.9.6, Robovm 2.3.2 or multi-os-engine v1.3.8.

### Supported  payment services

* **Google Play (Android)**: There are two implementations for Google Play Billing available:
[googlebilling](https://github.com/libgdx/gdx-pay/tree/master/gdx-pay-android-googlebilling) and [googleplay](https://github.com/libgdx/gdx-pay/tree/master/gdx-pay-android-googleplay)

* **Amazon IAP (Android)**: [amazon](https://github.com/libgdx/gdx-pay/tree/master/gdx-pay-android-amazon)

* **Apple (iOS)**: Two implementations available for [ios-moe](https://github.com/libgdx/gdx-pay/tree/master/gdx-pay-iosmoe-apple) and [RoboVM](https://github.com/libgdx/gdx-pay/tree/master/gdx-pay-iosrobovm-apple)

Click on the links to view the subproject's readme files for service-dependant information and artifacts.

### Installation

The recommended way to use gdx-pay is via dependency management with Gradle or Maven. Artifacts are available in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.badlogicgames.gdxpay%22). [Guide for using gdx-pay with Maven](https://github.com/libgdx/gdx-pay/wiki/Maven-Integration).

*project-root/build.gradle:*

    ext {
        gdxPayVersion = '0.11.2'
    }

Add the following dependencies:

*core:*

        compile "com.badlogicgames.gdxpay:gdx-pay-client:$gdxPayVersion"

*html:*

        compile "com.badlogicgames.gdxpay:gdx-pay:$gdxPayVersion:sources"
        compile "com.badlogicgames.gdxpay:gdx-pay-client:$gdxPayVersion:sources"

You also need to add the following file to your GdxDefinition.gwt.xml in your html project:

	    <inherits name="com.badlogic.gdx.pay_client"/>

That's all you need to use gdx-pay in the core project. Of course, you want to use a certain IAP service in your game.
Look in the service subproject's readme files linked above.
    
### Usage

...

### News & Community

Check the [libGDX blog](http://www.badlogicgames.com/) for news and updates.
You can get help on the [libGDX forum](http://www.badlogicgames.com/forum/) and talk to other users on the IRC channel #libgdx at irc.freenode.net.

### Reporting Issues

Something not working quite as expected? Do you need a feature that has not been implemented yet? Check the [issue tracker](https://github.com/libgdx/gdx-pay/issues) and add a new one if your problem is not already listed. Please try to provide a detailed description of your problem, including the steps to reproduce it.

### Using gdx-pay locally build binaries in a project

When Gradle is used to manage dependencies, gdx-pay locally build SNAPSHOT binaries can be used via the Maven local repository.

#### Install gdx-pay binaries in local Maven repository

Build gdx-pay with the following command:
`./gradlew assemble uploadArchives -PLOCAL`

#### Use Maven local SNAPSHOT version in a project 

1: add `mavenLocal()` as Gradle repository in the root `build.gradle`

    allprojects {
        repositories {
           mavenLocal()
        }
    }

2: change the version of gdx-pay dependencies to the `version` variable value found in gdx-pay/build.gradle

### Contributing

Awesome! If you would like to contribute with a new feature or a bugfix, [fork this repo](https://help.github.com/articles/fork-a-repo) and [submit a pull request](https://help.github.com/articles/using-pull-requests).
Also, before we can accept substantial code contributions, we need you to sign the [libGDX Contributor License Agreement](https://github.com/libgdx/libgdx/wiki/Contributing#contributor-license-agreement).

### License

The gdx-pay project is licensed under the [Apache 2 License](https://github.com/libgdx/gdx-pay/blob/master/LICENSE), meaning you can use it free of charge, without strings attached in commercial and non-commercial projects. We love to get (non-mandatory) credit in case you release a game or app using gdx-pay!

