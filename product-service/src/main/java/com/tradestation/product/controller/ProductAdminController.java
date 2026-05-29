package com.tradestation.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.product.entity.Product;
import com.tradestation.product.entity.ProductSku;
import com.tradestation.product.mapper.ProductMapper;
import com.tradestation.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/product/admin")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    private void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestHeader("X-User-Role") String role) {
        checkAdmin(role);
        Map<String, Object> result = new HashMap<>();
        java.util.List<Product> all = productMapper.selectList(null);
        result.put("totalProducts", all.size());
        Map<String, Long> byStatus = new HashMap<>();
        for (Product p : all) {
            byStatus.merge(p.getStatus(), 1L, Long::sum);
        }
        result.put("byStatus", byStatus);
        return Result.ok(result);
    }

    @GetMapping("/products")
    public Result<Page<Product>> listProducts(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String status) {
        checkAdmin(role);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Product::getStatus, "DELETED");
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getProductName, keyword);
        }
        if (shopId != null) {
            wrapper.eq(Product::getShopId, shopId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return Result.ok(productMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PutMapping("/products/{id}/status")
    public Result<Void> updateStatus(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        checkAdmin(role);
        Product product = productMapper.selectById(id);
        if (product == null || "DELETED".equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        product.setStatus(body.get("status"));
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        log.info("Admin updated product status: id={}, status={}", id, body.get("status"));
        return Result.ok();
    }

    // 获取商品详情（含 SKU 列表）
    @GetMapping("/products/{id}")
    public Result<Map<String, Object>> getProductDetail(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {
        checkAdmin(role);
        Product product = productMapper.selectById(id);
        if (product == null || "DELETED".equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 查询关联的 SKU
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductSku> skuWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        skuWrapper.eq(ProductSku::getProductId, id);
        java.util.List<ProductSku> skus = productSkuMapper.selectList(skuWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("skus", skus);
        return Result.ok(result);
    }

    // 完整编辑商品信息
    @PutMapping("/products/{id}")
    public Result<Void> updateProduct(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        checkAdmin(role);
        Product product = productMapper.selectById(id);
        if (product == null || "DELETED".equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (body.containsKey("productName")) {
            product.setProductName(String.valueOf(body.get("productName")));
        }
        if (body.containsKey("description")) {
            product.setDescription(String.valueOf(body.get("description")));
        }
        if (body.containsKey("categoryId")) {
            product.setCategoryId(Long.valueOf(String.valueOf(body.get("categoryId"))));
        }
        if (body.containsKey("mainImage")) {
            product.setMainImage(String.valueOf(body.get("mainImage")));
        }
        if (body.containsKey("status")) {
            product.setStatus(String.valueOf(body.get("status")));
        }
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);

        // 更新 SKU 价格
        if (body.containsKey("skus")) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> skuList =
                    (java.util.List<Map<String, Object>>) body.get("skus");
            for (Map<String, Object> skuData : skuList) {
                Long skuId = Long.valueOf(String.valueOf(skuData.get("id")));
                ProductSku sku = productSkuMapper.selectById(skuId);
                if (sku != null && sku.getProductId().equals(id)) {
                    if (skuData.containsKey("price")) {
                        sku.setPrice(new java.math.BigDecimal(String.valueOf(skuData.get("price"))));
                    }
                    if (skuData.containsKey("skuName")) {
                        sku.setSkuName(String.valueOf(skuData.get("skuName")));
                    }
                    sku.setUpdateTime(LocalDateTime.now());
                    productSkuMapper.updateById(sku);
                }
            }
        }

        log.info("Admin updated product detail: id={}", id);
        return Result.ok();
    }
}
