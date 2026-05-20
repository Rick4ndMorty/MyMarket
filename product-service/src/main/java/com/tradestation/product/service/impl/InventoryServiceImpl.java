package com.tradestation.product.service.impl;

import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.product.entity.Inventory;
import com.tradestation.product.mapper.InventoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradestation.product.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    @Resource
    private InventoryMapper inventoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deduct(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long skuId = toLong(item.get("skuId"));
            int quantity = toInt(item.get("quantity"));
            int version = toInt(item.get("version"));

            int affected = inventoryMapper.deduct(skuId, quantity, version);
            if (affected == 0) {
                Inventory inv = getBySkuId(skuId);
                if (inv == null || inv.getStock() < quantity) {
                    throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
                }
                // Version conflict (concurrent modification), treat as insufficient for caller to retry
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
            }
        }
        log.info("Inventory deducted: {} items", items.size());
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> restore(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long skuId = toLong(item.get("skuId"));
            int quantity = toInt(item.get("quantity"));

            int affected = inventoryMapper.restore(skuId, quantity);
            if (affected == 0) {
                log.warn("Restore affected 0 rows for skuId={}", skuId);
            }
        }
        log.info("Inventory restored: {} items", items.size());
        return Result.ok();
    }

    private Inventory getBySkuId(Long skuId) {
        return inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getSkuId, skuId)
        ).stream().findFirst().orElse(null);
    }

    private Long toLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        if (obj instanceof String) return Long.parseLong((String) obj);
        return (Long) obj;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) return Integer.parseInt((String) obj);
        return (Integer) obj;
    }
}
