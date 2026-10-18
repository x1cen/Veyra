package com.android.billingclient.api;

import java.util.ArrayList;
import java.util.List;

public class BillingFlowParams {
    public static Builder newBuilder() { return new Builder(); }

    public static class Builder {
        public Builder setProductDetailsParamsList(List<ProductDetailsParams> list) { return this; }
        public Builder setObfuscatedAccountId(String id) { return this; }
        public Builder setObfuscatedProfileId(String id) { return this; }
        public Builder setSubscriptionUpdateParams(SubscriptionUpdateParams params) { return this; }
        public BillingFlowParams build() { return new BillingFlowParams(); }
    }

    public static class ProductDetailsParams {
        private ProductDetails details;

        public static Builder newBuilder() { return new Builder(); }
        public static class Builder {
            private final ProductDetailsParams params = new ProductDetailsParams();
            public Builder setProductDetails(ProductDetails details) { params.details = details; return this; }
            public Builder setOfferToken(String token) { return this; }
            public ProductDetailsParams build() { return params; }
        }

        public ProductDetails zza() {
            return details != null ? details : new ProductDetails();
        }
    }

    public static class SubscriptionUpdateParams {
        public static final class ReplacementMode {
            public static final int CHARGE_FULL_PRICE = 5;
            public static final int CHARGE_PRORATED_PRICE = 2;
            public static final int DEFERRED = 6;
            public static final int WITHOUT_PRORATION = 3;
            public static final int UNKNOWN_REPLACEMENT_MODE = 0;
        }

        public static Builder newBuilder() { return new Builder(); }
        public static class Builder {
            public Builder setOldPurchaseToken(String token) { return this; }
            public Builder setReplaceProrationMode(int mode) { return this; }
            public Builder setSubscriptionReplacementMode(int mode) { return this; }
            public SubscriptionUpdateParams build() { return new SubscriptionUpdateParams(); }
        }
    }
}
