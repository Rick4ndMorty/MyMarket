package com.tradestation.shop.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.result.Result;
import com.tradestation.shop.entity.Shop;

public interface ShopService {

    Result<Void> apply(Long userId, String shopName, String logo, String description, String phone);

    Result<Shop> getById(Long id);

    Result<Shop> getByUserId(Long userId);

    Result<Void> update(Long userId, Long shopId, Shop update);

    Result<Page<Shop>> list(int page, int size);
}
