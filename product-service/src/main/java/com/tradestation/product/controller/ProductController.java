package com.tradestation.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.product.dto.ProductDetailVO;
import com.tradestation.product.dto.ProductSaveReq;
import com.tradestation.product.dto.ProductVO;
import com.tradestation.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ProductService productService;

    /**
     * Publish a new product. Requires X-User-Id header for shop context.
     */
    @PostMapping
    public Result<Void> publish(@RequestHeader("X-User-Id") Long userId,
                                @RequestParam Long shopId,
                                @RequestBody ProductSaveReq req) {
        log.info("Publish product: userId={}, shopId={}", userId, shopId);
        return productService.publish(shopId, req);
    }

    /**
     * Search products. Public, no auth required.
     */
    @GetMapping
    public Result<Page<ProductVO>> search(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long shopId,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return productService.search(keyword, shopId, categoryId, status, page, size);
    }

    /**
     * Get product detail. Public.
     */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> getDetail(@PathVariable Long id) {
        return productService.getDetail(id);
    }

    /**
     * Update a product.
     */
    @PutMapping("/{id}")
    public Result<Void> update(@RequestHeader("X-User-Id") Long userId,
                                @RequestParam Long shopId,
                                @PathVariable("id") Long productId,
                                @RequestBody ProductSaveReq req) {
        log.info("Update product: userId={}, shopId={}, productId={}", userId, shopId, productId);
        return productService.update(shopId, productId, req);
    }

    /**
     * Update product status (ON_SHELF / OFF_SHELF).
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@RequestHeader("X-User-Id") Long userId,
                                      @RequestParam Long shopId,
                                      @PathVariable("id") Long productId,
                                      @RequestParam String status) {
        log.info("Update product status: userId={}, shopId={}, productId={}, status={}", userId, shopId, productId, status);
        return productService.updateStatus(shopId, productId, status);
    }

    /**
     * Delete a product (soft delete).
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("X-User-Id") Long userId,
                               @RequestParam Long shopId,
                               @PathVariable("id") Long productId) {
        log.info("Delete product: userId={}, shopId={}, productId={}", userId, shopId, productId);
        return productService.delete(shopId, productId);
    }
}
