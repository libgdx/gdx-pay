package com.badlogic.gdx.pay;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

// TODO: fix unit testing in gdx-pay-client, move class to there
@RunWith(MockitoJUnitRunner.class)
public class PurchaseSystemTest {

    @Mock
    PurchaseManager purchaseManager;

    @Before
    public void setUp() throws Exception {
        PurchaseSystem.setManager(purchaseManager);
    }

    @Test
    public void shouldDisposeManagerOnAppRestart() throws Exception {

        PurchaseSystem.onAppRestarted();

        verify(purchaseManager).dispose();
    }
}