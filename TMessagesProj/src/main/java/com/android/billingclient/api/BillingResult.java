package com.android.billingclient.api;

public class BillingResult {
    private int responseCode = BillingClient.BillingResponseCode.BILLING_UNAVAILABLE;
    private String debugMessage = "";

    public int getResponseCode() { return responseCode; }
    public String getDebugMessage() { return debugMessage; }

    public static Builder newBuilder() { return new Builder(); }

    public static class Builder {
        private final BillingResult result = new BillingResult();
        public Builder setResponseCode(int code) { result.responseCode = code; return this; }
        public Builder setDebugMessage(String msg) { result.debugMessage = msg; return this; }
        public BillingResult build() { return result; }
    }
}
