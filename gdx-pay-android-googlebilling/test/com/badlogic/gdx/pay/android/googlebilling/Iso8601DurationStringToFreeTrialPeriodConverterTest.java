package com.badlogic.gdx.pay.android.googlebilling;

import com.badlogic.gdx.pay.FreeTrialPeriod;
import com.badlogic.gdx.pay.FreeTrialPeriod.PeriodUnit;
import org.junit.Test;

import static com.badlogic.gdx.pay.android.googlebilling.Iso8601DurationStringToFreeTrialPeriodConverter.convertToFreeTrialPeriod;
import static org.junit.Assert.*;

public class Iso8601DurationStringToFreeTrialPeriodConverterTest {

    @Test
    public void convertsStringWithFewDays() {

        final FreeTrialPeriod duration = convertToFreeTrialPeriod("P3D");

        assertEquals(3, duration.getNumberOfUnits());
        assertEquals(PeriodUnit.DAY, duration.getUnit());
    }

    @Test
    public void convertsStringWithMoreThenTenDays() {

        final FreeTrialPeriod duration = convertToFreeTrialPeriod("P14D");

        assertEquals(14, duration.getNumberOfUnits());
        assertEquals(PeriodUnit.DAY, duration.getUnit());
    }

    @Test
    public void convertsStringWitSixMonths() {

        final FreeTrialPeriod duration = convertToFreeTrialPeriod("P6M");

        assertEquals(6, duration.getNumberOfUnits());
        assertEquals(PeriodUnit.MONTH, duration.getUnit());
    }

    @Test
    public void convertsStringWithOneYear() {

        final FreeTrialPeriod duration = convertToFreeTrialPeriod("P1Y");

        assertEquals(1, duration.getNumberOfUnits());
        assertEquals(PeriodUnit.YEAR, duration.getUnit());
    }


}