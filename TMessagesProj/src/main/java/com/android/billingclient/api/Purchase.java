package com.android.billingclient.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Purchase {
    public int getPurchaseState() { return 1; }
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
