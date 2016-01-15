package com.badlogic.gdx.pay.android.googleplay.testdata;

import com.badlogic.gdx.pay.Information;

public class InformationObjectMother {

    public static Information informationFullEditionEntitlement() {
        return new Information("Buy full edition", "Access to all levels", "€ 1.00");
    }
}
