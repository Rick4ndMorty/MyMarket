package com.tradestation.shop.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.result.Result;
import com.tradestation.shop.entity.Shop;
import com.tradestation.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @PostMapping
    public Result<Void> apply(@RequestHeader("X-User-Id") Long userId,
                              @RequestBody Map<String, Object> body) {
        String shopName = (String) body.get("shopName");
        String logo = (String) body.get("logoUrl");
        String description = (String) body.get("description");
        String phone = (String) body.get("phone");
        return shopService.apply(userId, shopName, logo, description, phone);
    }

    @GetMapping("/{id}")
    public Result<Shop> getById(@PathVariable Long id) {
        return shopService.getById(id);
    }

    @GetMapping("/my")
    public Result<Shop> getByUserId(@RequestHeader("X-User-Id") Long userId) {
        return shopService.getByUserId(userId);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@RequestHeader("X-User-Id") Long userId,
                               @PathVariable Long id,
                               @RequestBody Shop update) {
        return shopService.update(userId, id, update);
    }

    @GetMapping
    public Result<Page<Shop>> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return shopService.list(page, size);
    }
}
