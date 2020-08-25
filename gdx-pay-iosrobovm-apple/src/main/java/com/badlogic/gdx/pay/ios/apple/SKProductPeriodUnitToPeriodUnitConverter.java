package com.badlogic.gdx.pay.ios.apple;

import com.badlogic.gdx.pay.FreeTrialPeriod;
import org.robovm.apple.storekit.SKProductPeriodUnit;

import java.util.HashMap;
import java.util.Map;

enum SKProductPeriodUnitToPeriodUnitConverter {
    ;

    private static final Map<SKProductPeriodUnit, FreeTrialPeriod.PeriodUnit> appleToGdxUnitMap = new HashMap<SKProductPeriodUnit, FreeTrialPeriod.PeriodUnit>();

    static {
        appleToGdxUnitMap.put(SKProductPeriodUnit.Day, FreeTrialPeriod.PeriodUnit.DAY);
        appleToGdxUnitMap.put(SKProductPeriodUnit.Week, FreeTrialPeriod.PeriodUnit.WEEK);
        appleToGdxUnitMap.put(SKProductPeriodUnit.Month, FreeTrialPeriod.PeriodUnit.MONTH);
        appleToGdxUnitMap.put(SKProductPeriodUnit.Year, FreeTrialPeriod.PeriodUnit.YEAR);
    }

    public static FreeTrialPeriod.PeriodUnit convertToPeriodUnit(SKProductPeriodUnit unit) {
        return appleToGdxUnitMap.get(unit);
    }

}
