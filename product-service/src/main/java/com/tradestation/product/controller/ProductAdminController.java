package com.tradestation.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.product.entity.Product;
import com.tradestation.product.mapper.ProductMapper;
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
        if (body.containsKey("status")) {
            product.setStatus(String.valueOf(body.get("status")));
        }
        if (body.containsKey("productName")) {
            product.setProductName(String.valueOf(body.get("productName")));
        }
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
        log.info("Admin updated product: id={}, status={}", id, product.getStatus());
        return Result.ok();
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
}
