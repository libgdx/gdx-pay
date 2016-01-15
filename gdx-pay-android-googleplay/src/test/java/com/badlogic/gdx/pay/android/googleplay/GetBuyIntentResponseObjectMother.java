package com.badlogic.gdx.pay.android.googleplay;

import android.app.PendingIntent;
import android.content.IntentSender;
import android.os.Bundle;

import static com.badlogic.gdx.pay.android.googleplay.ResponseCode.BILLING_RESPONSE_RESULT_OK;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Object mother for Buy intents.
 *
 * <p>Uses Mockito to instantiate objects that are hard to construct when Android environment is
 * not running.</p>
 */
public class GetBuyIntentResponseObjectMother {

    public static Bundle buyIntentResponseOk() {

        PendingIntent buyIntent = mock(PendingIntent.class);

        IntentSender intentSender = mock(IntentSender.class);

        when(buyIntent.getIntentSender()).thenReturn(intentSender);

        Bundle bundle = new Bundle();
        bundle.putInt(GoogleBillingConstants.RESPONSE_CODE, BILLING_RESPONSE_RESULT_OK.getCode());
        bundle.putParcelable("BUY_INTENT", buyIntent);
        return bundle;

    }
}
