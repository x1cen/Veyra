package com.android.billingclient.api;

public class ConsumeParams {
    public static Builder newBuilder() { return new Builder(); }
    public static class Builder {
        public Builder setPurchaseToken(String token) { return this; }
        public ConsumeParams build() { return new ConsumeParams(); }
    }
}
