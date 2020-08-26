package com.badlogic.gdx.pay;

import javax.annotation.Nonnull;

/**
 * Free trial support for subscriptions.
 *
 * <p>Subscriptions in App Store and Google Play can have a free trial period before starting the billing for the subscription.</p>
 */
public final class FreeTrialPeriod {

    private final int numberOfUnits;

    @Nonnull
    private final PeriodUnit unit;

    public enum PeriodUnit {
        DAY,
        MONTH,
        WEEK,
        YEAR;

        public static PeriodUnit parse(char character) {
            switch(character) {
                case 'D':
                    return DAY;
                case 'W':
                    return WEEK;
                case 'M':
                    return MONTH;
                case 'Y':
                    return YEAR;
                default:
                    throw new IllegalArgumentException("Character not mapped to PeriodUnit: " + character);
            }
        }
    }

    public FreeTrialPeriod(int numberOfUnits, PeriodUnit unit) {
        this.numberOfUnits = numberOfUnits;
        this.unit = unit;
    }

    public int getNumberOfUnits() {
        return numberOfUnits;
    }

    @Nonnull
    public PeriodUnit getUnit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FreeTrialPeriod that = (FreeTrialPeriod) o;

        if (numberOfUnits != that.numberOfUnits) return false;
        return unit == that.unit;
    }

    @Override
    public int hashCode() {
        int result = numberOfUnits;
        result = 31 * result + unit.hashCode();
        return result;
    }
}
