package com.badlogic.gdx.pay.android.googleplay.billing.converter;

import com.badlogic.gdx.pay.Transaction;

import org.junit.Test;

import java.util.Date;

import static com.badlogic.gdx.pay.Transaction.REVERSAL_TEXT_CANCELLED;
import static com.badlogic.gdx.pay.Transaction.REVERSAL_TEXT_REFUNDED;
import static com.badlogic.gdx.pay.android.googleplay.billing.converter.InAppPurchaseDataToTransactionConverter.convertJSONPurchaseToTransaction;
import static org.assertj.core.api.Assertions.assertThat;

public class InAppPurchaseDataToTransactionConverterTest {

    @Test
    public void fillsPurchaseFromJson() throws Exception {
        String payload = "{\"packageName\":\"com.app.name\",\"productId\":\"com.app.name.productId\",\n" +
                " \"purchaseTime\":1466539081315,\"purchaseState\":0,\n" +
                " \"developerPayload\":\"justSomePayload\",\n" +
                " \"purchaseToken\":\"randomToken\"}\n";

        Transaction transaction = convertJSONPurchaseToTransaction(payload);

        assertThat(transaction.getIdentifier()).isEqualTo("com.app.name.productId");
        assertThat(transaction.getPurchaseTime()).isWithinMonth(6);
        assertThat(transaction.getReversalTime()).isNull();
        assertThat(transaction.getReversalText()).isNull();
    }

    @Test
    public void marksTransactionAsReversedWhenPurchaseStateIsCancelled() throws Exception {
        String payload = "{\"packageName\":\"com.app.name\",\"productId\":\"com.app.name.productId\",\n" +
                " \"purchaseTime\":1466539081315,\"purchaseState\":1,\n" +
                " \"developerPayload\":\"justSomePayload\",\n" +
                " \"purchaseToken\":\"randomToken\"}\n";

        Transaction transaction = convertJSONPurchaseToTransaction(payload);

        assertThat(transaction.getReversalTime()).isCloseTo(new Date(), 100);
        assertThat(transaction.getReversalText()).isEqualTo(REVERSAL_TEXT_CANCELLED);
    }

    @Test
    public void marksTransactionAsReversedWhenPurchaseStateIsRefunded() throws Exception {
        String payload = "{\"packageName\":\"com.app.name\",\"productId\":\"com.app.name.productId\",\n" +
                " \"purchaseTime\":1466539081315,\"purchaseState\":2,\n" +
                " \"developerPayload\":\"justSomePayload\",\n" +
                " \"purchaseToken\":\"randomToken\"}\n";

        Transaction transaction = convertJSONPurchaseToTransaction(payload);

        assertThat(transaction.getReversalTime()).isCloseTo(new Date(), 100);
        assertThat(transaction.getReversalText()).isEqualTo(REVERSAL_TEXT_REFUNDED);
    }
}