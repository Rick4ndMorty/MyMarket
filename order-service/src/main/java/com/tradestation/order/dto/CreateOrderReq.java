package com.tradestation.order.dto;

import java.util.List;

public class CreateOrderReq {

    private List<OrderItemReq> items;
    private Long addressId;
    private String remark;

    public List<OrderItemReq> getItems() { return items; }
    public void setItems(List<OrderItemReq> items) { this.items = items; }

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public static class OrderItemReq {
        private Long skuId;
        private Integer quantity;

        public Long getSkuId() { return skuId; }
        public void setSkuId(Long skuId) { this.skuId = skuId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
