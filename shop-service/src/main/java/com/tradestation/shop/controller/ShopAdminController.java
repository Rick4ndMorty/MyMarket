package com.tradestation.shop.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.shop.entity.Shop;
import com.tradestation.shop.feign.ShopUserFeignClient;
import com.tradestation.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/shop/admin")
@RequiredArgsConstructor
public class ShopAdminController {

    private final ShopMapper shopMapper;
    private final ShopUserFeignClient userFeignClient;

    private void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestHeader("X-User-Role") String role) {
        checkAdmin(role);
        Map<String, Object> result = new HashMap<>();
        java.util.List<Shop> all = shopMapper.selectList(null);
        result.put("totalShops", all.size());
        Map<String, Long> byStatus = new HashMap<>();
        for (Shop s : all) {
            byStatus.merge(s.getStatus(), 1L, Long::sum);
        }
        result.put("byStatus", byStatus);
        return Result.ok(result);
    }

    @GetMapping("/shops")
    public Result<Page<Shop>> listShops(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        checkAdmin(role);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Shop::getShopName, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Shop::getStatus, status);
        }
        wrapper.orderByDesc(Shop::getCreateTime);
        return Result.ok(shopMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PutMapping("/shops/{id}/audit")
    public Result<Void> auditShop(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        checkAdmin(role);
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        if (!"PENDING".equals(shop.getStatus())) {
            throw new BusinessException(ErrorCode.SHOP_STATUS_INVALID, "shop is not in PENDING status");
        }
        String newStatus = String.valueOf(body.get("status"));
        if (!"ACTIVE".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status must be ACTIVE or REJECTED");
        }
        shop.setStatus(newStatus);
        if ("REJECTED".equals(newStatus) && body.get("rejectReason") != null) {
            shop.setRejectReason(String.valueOf(body.get("rejectReason")));
        }
        shop.setUpdateTime(LocalDateTime.now());
        shopMapper.updateById(shop);
        log.info("Admin audited shop: id={}, status={}", id, newStatus);

        // 审核通过 → 更新用户角色为 SELLER
        if ("ACTIVE".equals(newStatus)) {
            try {
                userFeignClient.updateRole(shop.getUserId(), Map.of("role", "SELLER"));
                log.info("User role updated to SELLER: userId={}", shop.getUserId());
            } catch (Exception e) {
                log.error("Failed to update user role: userId={}", shop.getUserId(), e);
            }
        }

        return Result.ok();
    }

    // 获取店铺详情
    @GetMapping("/shops/{id}")
    public Result<Shop> getShopDetail(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {
        checkAdmin(role);
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        return Result.ok(shop);
    }

    // 关闭店铺
    @PutMapping("/shops/{id}/close")
    public Result<Void> closeShop(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        checkAdmin(role);
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_FOUND);
        }
        if ("CLOSED".equals(shop.getStatus())) {
            throw new BusinessException(ErrorCode.SHOP_STATUS_INVALID, "店铺已关闭");
        }
        shop.setStatus("CLOSED");
        if (body != null && body.get("reason") != null) {
            shop.setRejectReason(String.valueOf(body.get("reason")));
        }
        shop.setUpdateTime(LocalDateTime.now());
        shopMapper.updateById(shop);
        log.info("Admin closed shop: id={}", id);
        return Result.ok();
    }
}
