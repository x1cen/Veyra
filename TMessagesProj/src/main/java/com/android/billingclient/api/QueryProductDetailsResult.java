package com.android.billingclient.api;

import java.util.Collections;
import java.util.List;

public class QueryProductDetailsResult {
    private final List<ProductDetails> productDetailsList;

    public QueryProductDetailsResult() {
        this.productDetailsList = Collections.emptyList();
    }

    public QueryProductDetailsResult(List<ProductDetails> list) {
        this.productDetailsList = list != null ? list : Collections.emptyList();
    }

    public List<ProductDetails> getProductDetailsList() {
        return productDetailsList;
    }
}
