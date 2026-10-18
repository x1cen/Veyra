package com.android.billingclient.api;

import java.util.List;

public interface PurchasesUpdatedListener {
    void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases);
}
