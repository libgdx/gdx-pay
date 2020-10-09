package com.badlogic.gdx.pay.android.googlebilling;

import com.badlogic.gdx.pay.SubscriptionPeriod;
import com.badlogic.gdx.pay.SubscriptionPeriod.PeriodUnit;
import org.junit.Test;

import static org.junit.Assert.*;

public class Iso8601DurationStringToSubscriptionPeriodConverterTest {

    @Test
    public void convertsStringWithFewDays() {

        final SubscriptionPeriod period = Iso8601DurationStringToFreeTrialPeriodConverter.convertToFreeTrialPeriod("P3D");

        assertEquals(3, period.getNumberOfUnits());
        assertEquals(PeriodUnit.DAY, period.getUnit());
    }

    @Test
    public void convertsStringWithMoreThenTenDays() {

        final SubscriptionPeriod period = Iso8601DurationStringToFreeTrialPeriodConverter.convertToFreeTrialPeriod("P14D");

        assertEquals(14, period.getNumberOfUnits());
        assertEquals(PeriodUnit.DAY, period.getUnit());
    }

    @Test
    public void convertsStringWitSixMonths() {

        final SubscriptionPeriod period = Iso8601DurationStringToFreeTrialPeriodConverter.convertToFreeTrialPeriod("P6M");

        assertEquals(6, period.getNumberOfUnits());
        assertEquals(PeriodUnit.MONTH, period.getUnit());
    }

    @Test
    public void convertsStringWithOneYear() {

        final SubscriptionPeriod period = Iso8601DurationStringToFreeTrialPeriodConverter.convertToFreeTrialPeriod("P1Y");

        assertEquals(1, period.getNumberOfUnits());
        assertEquals(PeriodUnit.YEAR, period.getUnit());
    }


}