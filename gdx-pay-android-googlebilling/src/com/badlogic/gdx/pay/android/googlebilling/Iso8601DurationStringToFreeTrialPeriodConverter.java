package com.badlogic.gdx.pay.android.googlebilling;

import com.badlogic.gdx.pay.SubscriptionPeriod;
import com.badlogic.gdx.pay.SubscriptionPeriod.PeriodUnit;

import javax.annotation.Nonnull;


class Iso8601DurationStringToFreeTrialPeriodConverter {

    /**
     * <p>See also:
     * <a href="https://www.digi.com/resources/documentation/digidocs/90001437-13/reference/r_iso_8601_duration_format.htm">
     *                        the spec</a>
     */
    @Nonnull
    public static SubscriptionPeriod convertToFreeTrialPeriod(@Nonnull String iso8601Duration) {
        final int numberOfUnits = Integer.parseInt(iso8601Duration.substring(1, iso8601Duration.length() -1 ));
        final PeriodUnit unit = PeriodUnit.parse(iso8601Duration.substring(iso8601Duration.length() - 1).charAt(0));

        return new SubscriptionPeriod(numberOfUnits, unit);
    }
}
