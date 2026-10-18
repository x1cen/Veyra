package com.android.billingclient.api;

import java.util.List;

public interface PurchasesResponseListener {
    void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases);
}
