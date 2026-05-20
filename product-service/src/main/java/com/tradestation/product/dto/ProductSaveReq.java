package com.tradestation.product.dto;

import java.util.List;

public class ProductSaveReq {

    private String productName;
    private String description;
    private String mainImage;
    private List<String> images;
    private Long categoryId;
    private List<SkuSaveReq> skus;

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<SkuSaveReq> getSkus() { return skus; }
    public void setSkus(List<SkuSaveReq> skus) { this.skus = skus; }
}
