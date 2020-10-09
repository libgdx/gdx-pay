package com.badlogic.gdx.pay.ios.apple;

import com.badlogic.gdx.pay.SubscriptionPeriod;
import org.robovm.apple.storekit.SKProductPeriodUnit;

import java.util.HashMap;
import java.util.Map;

enum SKProductPeriodUnitToPeriodUnitConverter {
    ;

    private static final Map<SKProductPeriodUnit, SubscriptionPeriod.PeriodUnit> appleToGdxUnitMap = new HashMap<SKProductPeriodUnit, SubscriptionPeriod.PeriodUnit>();

    static {
        appleToGdxUnitMap.put(SKProductPeriodUnit.Day, SubscriptionPeriod.PeriodUnit.DAY);
        appleToGdxUnitMap.put(SKProductPeriodUnit.Week, SubscriptionPeriod.PeriodUnit.WEEK);
        appleToGdxUnitMap.put(SKProductPeriodUnit.Month, SubscriptionPeriod.PeriodUnit.MONTH);
        appleToGdxUnitMap.put(SKProductPeriodUnit.Year, SubscriptionPeriod.PeriodUnit.YEAR);
    }

    public static SubscriptionPeriod.PeriodUnit convertToPeriodUnit(SKProductPeriodUnit unit) {
        return appleToGdxUnitMap.get(unit);
    }

}
