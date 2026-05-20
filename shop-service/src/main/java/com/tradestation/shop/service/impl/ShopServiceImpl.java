package com.tradestation.shop.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.shop.entity.Shop;
import com.tradestation.shop.mapper.ShopMapper;
import com.tradestation.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;

    @Override
    @SentinelResource(value = "shopApply", blockHandler = "shopApplyBlockHandler")
    public Result<Void> apply(Long userId, String shopName, String logo, String description, String phone) {
        // 检查是否已有审核中或已激活的店铺，CLOSED/REJECTED 可重新申请
        LambdaQueryWrapper<Shop> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(Shop::getUserId, userId)
                   .in(Shop::getStatus, "PENDING", "ACTIVE");
        if (shopMapper.selectCount(userWrapper) > 0) {
            throw new BusinessException(ErrorCode.SHOP_ALREADY_EXISTS, "您已拥有店铺，无法重复申请");
        }

        // 检查店铺名是否已被使用
        LambdaQueryWrapper<Shop> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(Shop::getShopName, shopName);
        if (shopMapper.selectCount(nameWrapper) > 0) {
            throw new BusinessException(ErrorCode.SHOP_ALREADY_EXISTS, "店铺名已被使用");
        }

        Shop shop = new Shop();
        shop.setUserId(userId);
        shop.setShopName(shopName);
        shop.setLogo(logo);
        shop.setDescription(description);
        shop.setPhone(phone);
        shop.setStatus("PENDING");
        shop.setCreateTime(LocalDateTime.now());
        shop.setUpdateTime(LocalDateTime.now());

        shopMapper.insert(shop);

        log.info("Shop application submitted: userId={}, shopId={}", userId, shop.getId());
        return Result.ok();
    }

    @Override
    public Result<Shop> getById(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        return Result.ok(shop);
    }

    @Override
    public Result<Shop> getByUserId(Long userId) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getUserId, userId)
               .orderByDesc(Shop::getCreateTime)
               .last("LIMIT 1");
        Shop shop = shopMapper.selectOne(wrapper);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        return Result.ok(shop);
    }

    @Override
    public Result<Void> update(Long userId, Long shopId, Shop update) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        if (!shop.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (update.getShopName() != null) {
            shop.setShopName(update.getShopName());
        }
        if (update.getLogo() != null) {
            shop.setLogo(update.getLogo());
        }
        if (update.getDescription() != null) {
            shop.setDescription(update.getDescription());
        }
        if (update.getPhone() != null) {
            shop.setPhone(update.getPhone());
        }

        shopMapper.updateById(shop);

        log.info("Shop updated: shopId={}, userId={}", shopId, userId);
        return Result.ok();
    }

    @Override
    public Result<Page<Shop>> list(int page, int size) {
        Page<Shop> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getStatus, "ACTIVE")
               .orderByDesc(Shop::getCreateTime);

        Page<Shop> result = shopMapper.selectPage(pageParam, wrapper);
        return Result.ok(result);
    }

    public Result<Void> shopApplyBlockHandler(Long userId, String shopName, String logo, String description, String phone, BlockException e) {
        log.error("(触发限流)shopApply blocked by Sentinel: userId={}, shopName={}", userId, shopName, e);
        return Result.fail(ErrorCode.RATE_LIMITED);
    }
}
