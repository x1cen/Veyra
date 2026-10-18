package com.android.billingclient.api;

import java.util.Collections;
import java.util.List;

public class ProductDetails {
    public String getProductId() { return ""; }
    public String getProductType() { return ""; }
    public String getTitle() { return ""; }
    public String getName() { return ""; }
    public String getDescription() { return ""; }
    public OneTimePurchaseOfferDetails getOneTimePurchaseOfferDetails() { return null; }
    public List<SubscriptionOfferDetails> getSubscriptionOfferDetails() { return Collections.emptyList(); }

    public static class OneTimePurchaseOfferDetails {
        public long getPriceAmountMicros() { return 0; }
        public String getFormattedPrice() { return ""; }
        public String getPriceCurrencyCode() { return ""; }
    }

    public static class SubscriptionOfferDetails {
        public String getOfferToken() { return ""; }
        public String getOfferId() { return ""; }
        public PricingPhases getPricingPhases() { return new PricingPhases(); }
    }

    public static class PricingPhases {
        public List<PricingPhase> getPricingPhaseList() { return Collections.emptyList(); }
    }

    public static class PricingPhase {
        public long getPriceAmountMicros() { return 0; }
        public String getFormattedPrice() { return ""; }
        public String getPriceCurrencyCode() { return ""; }
        public String getBillingPeriod() { return ""; }
        public int getRecurrenceMode() { return 0; }
        public int getBillingCycleCount() { return 0; }
    }
}
