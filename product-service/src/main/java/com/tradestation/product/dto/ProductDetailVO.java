package com.tradestation.product.dto;

import java.util.List;

public class ProductDetailVO extends ProductVO {

    private List<SkuVO> skus;

    public List<SkuVO> getSkus() { return skus; }
    public void setSkus(List<SkuVO> skus) { this.skus = skus; }
}
