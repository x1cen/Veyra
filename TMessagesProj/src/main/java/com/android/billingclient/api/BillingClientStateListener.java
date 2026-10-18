package com.android.billingclient.api;

public interface BillingClientStateListener {
    void onBillingSetupFinished(BillingResult billingResult);
    void onBillingServiceDisconnected();
}
