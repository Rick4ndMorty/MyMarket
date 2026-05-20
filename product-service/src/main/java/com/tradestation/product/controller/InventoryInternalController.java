package com.tradestation.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradestation.common.result.Result;
import com.tradestation.product.dto.SkuVO;
import com.tradestation.product.entity.Inventory;
import com.tradestation.product.entity.Product;
import com.tradestation.product.entity.ProductSku;
import com.tradestation.product.mapper.InventoryMapper;
import com.tradestation.product.mapper.ProductMapper;
import com.tradestation.product.mapper.ProductSkuMapper;
import com.tradestation.product.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Internal endpoints called by order-service via Feign.
 * Not exposed to the gateway.
 */
@Slf4j
@RestController
@RequestMapping("/product")
public class InventoryInternalController {

    @Resource
    private InventoryService inventoryService;
    @Resource
    private ProductSkuMapper productSkuMapper;
    @Resource
    private InventoryMapper inventoryMapper;
    @Resource
    private ProductMapper productMapper;

    /**
     * Deduct inventory for an order. Uses optimistic locking.
     */
    @PutMapping("/inventory/deduct")
    public Result<Void> deduct(@RequestBody List<Map<String, Object>> items) {
        log.info("Deduct inventory: {} items", items.size());
        return inventoryService.deduct(items);
    }

    /**
     * Restore inventory when order is cancelled.
     */
    @PutMapping("/inventory/restore")
    public Result<Void> restore(@RequestBody List<Map<String, Object>> items) {
        log.info("Restore inventory: {} items", items.size());
        return inventoryService.restore(items);
    }

    /**
     * Batch query SKUs with inventory info.
     */
    @GetMapping("/sku/batch")
    public Result<List<SkuVO>> batchQuery(@RequestParam("ids") String idsStr) {
        List<Long> skuIds = Arrays.stream(idsStr.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<ProductSku> skus = productSkuMapper.selectBatchIds(skuIds);

        // Load products for shopId
        List<Long> productIds = skus.stream()
                .map(ProductSku::getProductId)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        // Load inventories
        LambdaQueryWrapper<Inventory> invWrapper = new LambdaQueryWrapper<>();
        invWrapper.in(Inventory::getSkuId, skuIds);
        List<Inventory> inventories = inventoryMapper.selectList(invWrapper);
        Map<Long, Inventory> invMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getSkuId, inv -> inv, (a, b) -> a));

        List<SkuVO> voList = skus.stream().map(sku -> {
            SkuVO vo = new SkuVO();
            BeanUtils.copyProperties(sku, vo);
            Product product = productMap.get(sku.getProductId());
            if (product != null) {
                vo.setShopId(product.getShopId());
            }
            Inventory inv = invMap.get(sku.getId());
            if (inv != null) {
                vo.setStock(inv.getStock());
                vo.setLockedStock(inv.getLockedStock());
                vo.setVersion(inv.getVersion());
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(voList);
    }
}
