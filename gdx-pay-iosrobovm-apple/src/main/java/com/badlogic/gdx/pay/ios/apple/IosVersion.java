package com.badlogic.gdx.pay.ios.apple;

import org.robovm.apple.foundation.Foundation;

enum IosVersion {
    ;

    static boolean is_7_0_orAbove() {
        return Foundation.getMajorSystemVersion() >= 7;
    }

    static boolean is_11_2_orAbove() {
        return ((Foundation.getMajorSystemVersion()  == 11 && Foundation.getMinorSystemVersion() >= 2)
                || Foundation.getMajorSystemVersion() > 11);
    }

    static boolean is_16_4_orAbove() {
        return ((Foundation.getMajorSystemVersion() == 16 && Foundation.getMinorSystemVersion() >= 4)
                || Foundation.getMajorSystemVersion() > 16);
    }
}
