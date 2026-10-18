package com.android.billingclient.api;

import java.util.List;

public interface ProductDetailsResponseListener {
    void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> productDetailsList);
}
