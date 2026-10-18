package com.android.billingclient.api;

import java.util.Collections;
import java.util.List;

public class Purchase {
    public static final class PurchaseState {
        public static final int UNSPECIFIED_STATE = 0;
        public static final int PURCHASED = 1;
        public static final int PENDING = 2;
    }

    public int getPurchaseState() { return PurchaseState.PURCHASED; }
    public String getPurchaseToken() { return ""; }
    public String getPackageName() { return ""; }
    public List<String> getProducts() { return Collections.emptyList(); }
    public String getOrderId() { return ""; }
    public long getPurchaseTime() { return 0; }
    public boolean isAcknowledged() { return true; }
    public AccountIdentifiers getAccountIdentifiers() { return null; }
    public String getOriginalJson() { return "{}"; }
    public String getSignature() { return ""; }
}
