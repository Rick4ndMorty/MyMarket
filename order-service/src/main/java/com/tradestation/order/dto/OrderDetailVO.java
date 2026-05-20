package com.tradestation.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderDetailVO extends OrderVO {

    private Map<String, Object> addressSnapshot;
    private List<OrderItemVO> items;

    public Map<String, Object> getAddressSnapshot() { return addressSnapshot; }
    public void setAddressSnapshot(Map<String, Object> addressSnapshot) { this.addressSnapshot = addressSnapshot; }

    public List<OrderItemVO> getItems() { return items; }
    public void setItems(List<OrderItemVO> items) { this.items = items; }

    public static class OrderItemVO {
        private Long id;
        private Long orderId;
        private Long skuId;
        private Map<String, Object> skuSnapshot;
        private Integer quantity;
        private BigDecimal unitPrice;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public Long getSkuId() { return skuId; }
        public void setSkuId(Long skuId) { this.skuId = skuId; }

        public Map<String, Object> getSkuSnapshot() { return skuSnapshot; }
        public void setSkuSnapshot(Map<String, Object> skuSnapshot) { this.skuSnapshot = skuSnapshot; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }
}
