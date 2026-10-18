package com.android.billingclient.api;

public class QueryPurchasesParams {
    public static Builder newBuilder() { return new Builder(); }
    public static class Builder {
        public Builder setProductType(String type) { return this; }
        public QueryPurchasesParams build() { return new QueryPurchasesParams(); }
    }
}
