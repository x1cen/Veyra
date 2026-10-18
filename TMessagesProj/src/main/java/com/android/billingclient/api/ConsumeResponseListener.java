package com.android.billingclient.api;

public interface ConsumeResponseListener {
    void onConsumeResponse(BillingResult billingResult, String purchaseToken);
}
