package com.badlogic.gdx.pay.ios.apple;

import apple.foundation.NSProcessInfo;
import apple.foundation.struct.NSOperatingSystemVersion;

enum IosVersion {
    ;

    static boolean is_7_0_orAbove() {
        return NSProcessInfo.alloc().operatingSystemVersion().majorVersion() >= 7;
    }

    static boolean is_11_2_orAbove() {
        return ((NSProcessInfo.alloc().operatingSystemVersion().majorVersion()  == 11
                && NSProcessInfo.alloc().operatingSystemVersion().minorVersion() >= 2)
                || NSProcessInfo.alloc().operatingSystemVersion().majorVersion() > 11);
    }
}