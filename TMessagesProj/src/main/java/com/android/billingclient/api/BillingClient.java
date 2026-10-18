package com.android.billingclient.api;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.List;

public abstract class BillingClient {
    public static final class ConnectionState {
        public static final int CONNECTED = 2;
        public static final int DISCONNECTED = 0;
    }
    public static final class BillingResponseCode {
        public static final int SERVICE_TIMEOUT = -3;
        public static final int FEATURE_NOT_SUPPORTED = -2;
        public static final int SERVICE_DISCONNECTED = -1;
        public static final int OK = 0;
        public static final int USER_CANCELED = 1;
        public static final int SERVICE_UNAVAILABLE = 2;
        public static final int BILLING_UNAVAILABLE = 3;
        public static final int ITEM_UNAVAILABLE = 4;
        public static final int DEVELOPER_ERROR = 5;
        public static final int ERROR = 6;
        public static final int ITEM_ALREADY_OWNED = 7;
        public static final int ITEM_NOT_OWNED = 8;
        public static final int NETWORK_ERROR = 12;
    }
    public static final class ProductType {
        public static final String INAPP = "inapp";
        public static final String SUBS = "subs";
    }

    public static Builder newBuilder(@NonNull Context context) {
        return new Builder();
    }

    public static final class Builder {
        public Builder setListener(PurchasesUpdatedListener listener) { return this; }
        public Builder enablePendingPurchases(PendingPurchasesParams params) { return this; }
        public BillingClient build() { return new BillingClientStub(); }
    }

    public abstract void startConnection(BillingClientStateListener listener);
    public abstract void endConnection();
    public abstract boolean isReady();
    public abstract int getConnectionState();
    public abstract void queryProductDetailsAsync(QueryProductDetailsParams params, ProductDetailsResponseListener listener);
    public abstract void queryPurchasesAsync(QueryPurchasesParams params, PurchasesResponseListener listener);
    public abstract void consumeAsync(ConsumeParams params, ConsumeResponseListener listener);
    public abstract BillingResult launchBillingFlow(Activity activity, BillingFlowParams params);

    private static class BillingClientStub extends BillingClient {
        public void startConnection(BillingClientStateListener listener) {
            if (listener != null) listener.onBillingServiceDisconnected();
        }
        public void endConnection() {}
        public boolean isReady() { return false; }
        public int getConnectionState() { return ConnectionState.DISCONNECTED; }
        public void queryProductDetailsAsync(QueryProductDetailsParams params, ProductDetailsResponseListener listener) {
            if (listener != null) listener.onProductDetailsResponse(BillingResult.newBuilder().setResponseCode(BillingResponseCode.BILLING_UNAVAILABLE).build(), new QueryProductDetailsResult());
        }
        public void queryPurchasesAsync(QueryPurchasesParams params, PurchasesResponseListener listener) {
            if (listener != null) listener.onQueryPurchasesResponse(BillingResult.newBuilder().setResponseCode(BillingResponseCode.BILLING_UNAVAILABLE).build(), Collections.emptyList());
        }
        public void consumeAsync(ConsumeParams params, ConsumeResponseListener listener) {
            if (listener != null) listener.onConsumeResponse(BillingResult.newBuilder().setResponseCode(BillingResponseCode.BILLING_UNAVAILABLE).build(), "");
        }
        public BillingResult launchBillingFlow(Activity activity, BillingFlowParams params) {
            return BillingResult.newBuilder().setResponseCode(BillingResponseCode.BILLING_UNAVAILABLE).build();
        }
    }
}
