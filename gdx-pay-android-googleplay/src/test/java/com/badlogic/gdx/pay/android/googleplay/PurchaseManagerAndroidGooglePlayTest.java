package com.badlogic.gdx.pay.android.googleplay;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class PurchaseManagerAndroidGooglePlayTest {

    private static final int REQUEST_CODE = 1032;

    @Mock
    Activity activity;
    private PurchaseManagerAndroidGooglePlay purchaseManager;

    @Before
    public void setUp() throws Exception {
        purchaseManager = new PurchaseManagerAndroidGooglePlay(activity, REQUEST_CODE);
    }

    @Test
    public void shouldNotBeInstalledAfterInstantiation() throws Exception {
        assertFalse(purchaseManager.installed());
    }
}