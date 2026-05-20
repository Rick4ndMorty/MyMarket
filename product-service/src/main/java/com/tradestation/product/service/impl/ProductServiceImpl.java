package com.tradestation.product.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.product.dto.*;
import com.tradestation.product.entity.Inventory;
import com.tradestation.product.entity.Product;
import com.tradestation.product.entity.ProductSku;
import com.tradestation.product.mapper.InventoryMapper;
import com.tradestation.product.mapper.ProductMapper;
import com.tradestation.product.mapper.ProductSkuMapper;
import com.tradestation.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private ProductMapper productMapper;
    @Resource
    private ProductSkuMapper productSkuMapper;
    @Resource
    private InventoryMapper inventoryMapper;

    @Override
    @SentinelResource(value = "publish", blockHandler = "publishBlockHandler")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> publish(Long shopId, ProductSaveReq req) {
        LocalDateTime now = LocalDateTime.now();

        Product product = new Product();
        product.setShopId(shopId);
        product.setProductName(req.getProductName());
        product.setDescription(req.getDescription());
        product.setMainImage(req.getMainImage());
        product.setImages(toJson(req.getImages()));
        product.setCategoryId(req.getCategoryId());
        product.setStatus("ON_SHELF");
        product.setCreateTime(now);
        product.setUpdateTime(now);
        productMapper.insert(product);

        List<SkuSaveReq> skus = req.getSkus();
        if (skus != null) {
            for (int i = 0; i < skus.size(); i++) {
                SkuSaveReq skuReq = skus.get(i);
                ProductSku sku = new ProductSku();
                sku.setProductId(product.getId());
                sku.setSkuCode(generateSkuCode(product.getId(), i));
                sku.setSkuName(skuReq.getSkuName());
                sku.setSpecifications(toJson(skuReq.getSpecifications()));
                sku.setPrice(skuReq.getPrice());
                sku.setImage(skuReq.getImage());
                sku.setCreateTime(now);
                sku.setUpdateTime(now);
                productSkuMapper.insert(sku);

                Inventory inventory = new Inventory();
                inventory.setSkuId(sku.getId());
                inventory.setStock(skuReq.getStock() != null ? skuReq.getStock() : 0);
                inventory.setLockedStock(0);
                inventory.setVersion(0);
                inventory.setCreateTime(now);
                inventory.setUpdateTime(now);
                inventoryMapper.insert(inventory);
            }
        }

        log.info("Product published: id={}, shopId={}, name={}", product.getId(), shopId, product.getProductName());
        return Result.ok();
    }

    @Override
    @SentinelResource(value = "productSearch", blockHandler = "searchBlockHandler")
    public Result<Page<ProductVO>> search(String keyword, Long shopId, Long categoryId,
                                          String status, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Product::getStatus, "DELETED");
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getProductName, keyword);
        }
        if (shopId != null) {
            wrapper.eq(Product::getShopId, shopId);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if ("ALL".equalsIgnoreCase(status)) {
            // skip status filter — show all non-deleted
        } else if (StringUtils.hasText(status)) {
            wrapper.eq(Product::getStatus, status);
        } else {
            // public browsing — only show on-shelf products
            wrapper.eq(Product::getStatus, "ON_SHELF");
        }
        wrapper.orderByDesc(Product::getCreateTime);

        Page<Product> productPage = productMapper.selectPage(new Page<>(page, size), wrapper);
        List<Product> products = productPage.getRecords();

        List<ProductVO> voList = new ArrayList<>();
        if (!products.isEmpty()) {
            List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            // Query min/max price per product from SKU table
            Map<Long, BigDecimal[]> priceMap = queryMinMaxPrices(productIds);

            for (Product p : products) {
                ProductVO vo = new ProductVO();
                BeanUtils.copyProperties(p, vo);
                vo.setImages(parseImages(p.getImages()));
                BigDecimal[] prices = priceMap.get(p.getId());
                if (prices != null) {
                    vo.setMinPrice(prices[0]);
                    vo.setMaxPrice(prices[1]);
                }
                voList.add(vo);
            }
        }

        Page<ProductVO> resultPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        resultPage.setRecords(voList);
        return Result.ok(resultPage);
    }

    @Override
    public Result<ProductDetailVO> getDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || "DELETED".equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        ProductDetailVO vo = new ProductDetailVO();
        BeanUtils.copyProperties(product, vo);
        vo.setImages(parseImages(product.getImages()));

        // Load SKUs
        LambdaQueryWrapper<ProductSku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.eq(ProductSku::getProductId, id);
        List<ProductSku> skus = productSkuMapper.selectList(skuWrapper);

        List<SkuVO> skuVOList = new ArrayList<>();
        if (!skus.isEmpty()) {
            List<Long> skuIds = skus.stream().map(ProductSku::getId).collect(Collectors.toList());

            // Load inventories in batch
            LambdaQueryWrapper<Inventory> invWrapper = new LambdaQueryWrapper<>();
            invWrapper.in(Inventory::getSkuId, skuIds);
            List<Inventory> inventories = inventoryMapper.selectList(invWrapper);
            Map<Long, Inventory> invMap = inventories.stream()
                    .collect(Collectors.toMap(Inventory::getSkuId, inv -> inv, (a, b) -> a));

            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;

            for (ProductSku sku : skus) {
                SkuVO skuVO = new SkuVO();
                BeanUtils.copyProperties(sku, skuVO);
                skuVO.setShopId(product.getShopId());
                Inventory inv = invMap.get(sku.getId());
                if (inv != null) {
                    skuVO.setStock(inv.getStock());
                    skuVO.setLockedStock(inv.getLockedStock());
                }
                skuVOList.add(skuVO);

                if (sku.getPrice() != null) {
                    if (minPrice == null || sku.getPrice().compareTo(minPrice) < 0) {
                        minPrice = sku.getPrice();
                    }
                    if (maxPrice == null || sku.getPrice().compareTo(maxPrice) > 0) {
                        maxPrice = sku.getPrice();
                    }
                }
            }

            vo.setMinPrice(minPrice);
            vo.setMaxPrice(maxPrice);
        }

        vo.setSkus(skuVOList);
        return Result.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long shopId, Long productId, ProductSaveReq req) {
        Product product = productMapper.selectById(productId);
        if (product == null || "DELETED".equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!product.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();
        product.setProductName(req.getProductName());
        product.setDescription(req.getDescription());
        product.setMainImage(req.getMainImage());
        product.setImages(toJson(req.getImages()));
        product.setCategoryId(req.getCategoryId());
        product.setUpdateTime(now);
        productMapper.updateById(product);

        List<SkuSaveReq> skuReqs = req.getSkus();
        if (skuReqs != null && !skuReqs.isEmpty()) {
            // Delete existing SKUs and inventories
            LambdaQueryWrapper<ProductSku> skuWrapper = new LambdaQueryWrapper<>();
            skuWrapper.eq(ProductSku::getProductId, productId);
            List<ProductSku> existingSkus = productSkuMapper.selectList(skuWrapper);
            if (!existingSkus.isEmpty()) {
                List<Long> skuIds = existingSkus.stream().map(ProductSku::getId).collect(Collectors.toList());
                LambdaQueryWrapper<Inventory> invWrapper = new LambdaQueryWrapper<>();
                invWrapper.in(Inventory::getSkuId, skuIds);
                inventoryMapper.delete(invWrapper);
                productSkuMapper.delete(skuWrapper);
            }

            // Re-create SKUs and inventories
            for (int i = 0; i < skuReqs.size(); i++) {
                SkuSaveReq skuReq = skuReqs.get(i);
                ProductSku sku = new ProductSku();
                sku.setProductId(productId);
                sku.setSkuCode(generateSkuCode(productId, i));
                sku.setSkuName(skuReq.getSkuName());
                sku.setSpecifications(toJson(skuReq.getSpecifications()));
                sku.setPrice(skuReq.getPrice());
                sku.setImage(skuReq.getImage());
                sku.setCreateTime(now);
                sku.setUpdateTime(now);
                productSkuMapper.insert(sku);

                Inventory inventory = new Inventory();
                inventory.setSkuId(sku.getId());
                inventory.setStock(skuReq.getStock() != null ? skuReq.getStock() : 0);
                inventory.setLockedStock(0);
                inventory.setVersion(0);
                inventory.setCreateTime(now);
                inventory.setUpdateTime(now);
                inventoryMapper.insert(inventory);
            }
        }

        log.info("Product updated: id={}, shopId={}", productId, shopId);
        return Result.ok();
    }

    @Override
    public Result<Void> updateStatus(Long shopId, Long productId, String status) {
        Product product = productMapper.selectById(productId);
        if (product == null || "DELETED".equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!product.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        product.setStatus(status);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);

        log.info("Product status updated: id={}, status={}", productId, status);
        return Result.ok();
    }

    @Override
    public Result<Void> delete(Long shopId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || "DELETED".equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!product.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        product.setStatus("DELETED");
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);

        log.info("Product deleted: id={}", productId);
        return Result.ok();
    }

    // ---- helper methods ----

    private String generateSkuCode(Long productId, int index) {
        return String.format("SKU_%d_%d", productId, index + 1);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization failed", e);
            return null;
        }
    }

    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isEmpty()) return Collections.emptyList();
        try {
            return OBJECT_MAPPER.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse images JSON: {}", imagesJson);
            return Collections.emptyList();
        }
    }

    private Map<Long, BigDecimal[]> queryMinMaxPrices(List<Long> productIds) {
        if (productIds.isEmpty()) return Collections.emptyMap();
        QueryWrapper<ProductSku> wrapper = new QueryWrapper<>();
        wrapper.select("product_id", "MIN(price) AS min_price", "MAX(price) AS max_price")
                .in("product_id", productIds)
                .groupBy("product_id");
        List<Map<String, Object>> rows = productSkuMapper.selectMaps(wrapper);

        Map<Long, BigDecimal[]> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long pid = ((Number) row.get("product_id")).longValue();
            Object minObj = row.get("min_price");
            Object maxObj = row.get("max_price");
            BigDecimal min = minObj != null ? new BigDecimal(minObj.toString()) : null;
            BigDecimal max = maxObj != null ? new BigDecimal(maxObj.toString()) : null;
            result.put(pid, new BigDecimal[]{min, max});
        }
        return result;
    }

    // ---- Sentinel block handlers ----

    public Result<Void> publishBlockHandler(Long shopId, ProductSaveReq req, BlockException e) {
        log.error("(触发限流)publish blocked by Sentinel: shopId={}", shopId, e);
        return Result.fail(ErrorCode.RATE_LIMITED);
    }

    public Result<Page<ProductVO>> searchBlockHandler(String keyword, Long shopId, Long categoryId, String status, int page, int size, BlockException e) {
        log.error("(触发限流)productSearch blocked by Sentinel: keyword={}", keyword, e);
        return Result.fail(ErrorCode.RATE_LIMITED);
    }
}
