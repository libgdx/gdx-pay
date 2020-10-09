package com.badlogic.gdx.pay.ios.apple;

import org.robovm.apple.foundation.Foundation;

enum IosVersion {
    ;

    static boolean is_7_0_orAbove() {
        return Foundation.getMajorSystemVersion() >= 7;
    }

    static boolean is_11_2_OrAbove() {
        return ((Foundation.getMajorSystemVersion()  == 11 && Foundation.getMinorSystemVersion() >= 2)
                || Foundation.getMajorSystemVersion() > 11);
    }
}
