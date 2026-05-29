package com.tradestation.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.user.entity.User;
import com.tradestation.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/user/admin")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserMapper userMapper;

    private void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestHeader("X-User-Role") String role) {
        checkAdmin(role);
        Map<String, Object> result = new HashMap<>();
        List<User> all = userMapper.selectList(null);
        result.put("totalUsers", all.size());
        result.put("byRole", all.stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting())));
        result.put("activeUsers", all.stream().filter(u -> u.getStatus() == 1).count());
        return Result.ok(result);
    }

    @GetMapping("/users")
    public Result<Page<User>> listUsers(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String filterRole,
            @RequestParam(required = false) Integer status) {
        checkAdmin(role);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (StringUtils.hasText(filterRole)) {
            wrapper.eq(User::getRole, filterRole);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(result);
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        checkAdmin(role);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (body.containsKey("role")) {
            user.setRole(String.valueOf(body.get("role")));
        }
        if (body.containsKey("status")) {
            user.setStatus(Integer.valueOf(String.valueOf(body.get("status"))));
        }
        if (body.containsKey("username")) {
            user.setUsername(String.valueOf(body.get("username")));
        }
        if (body.containsKey("email")) {
            user.setEmail(String.valueOf(body.get("email")));
        }
        if (body.containsKey("phone")) {
            user.setPhone(String.valueOf(body.get("phone")));
        }
        userMapper.updateById(user);
        log.info("Admin updated user: id={}, role={}, status={}", id, user.getRole(), user.getStatus());
        return Result.ok();
    }

    // 删除用户（软删除：设置 status=0 禁用）
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {
        checkAdmin(role);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能删除管理员账号");
        }
        user.setStatus(0); // 软删除
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);
        log.info("Admin deleted (disabled) user: id={}", id);
        return Result.ok();
    }

    // 批量更新用户状态
    @PutMapping("/users/batch/status")
    public Result<Void> batchUpdateStatus(
            @RequestHeader("X-User-Role") String role,
            @RequestBody Map<String, Object> body) {
        checkAdmin(role);
        @SuppressWarnings("unchecked")
        java.util.List<Integer> ids = (java.util.List<Integer>) body.get("ids");
        Integer status = Integer.valueOf(String.valueOf(body.get("status")));
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ids 不能为空");
        }
        for (Integer uid : ids) {
            User user = userMapper.selectById(uid);
            if (user != null && !"ADMIN".equals(user.getRole())) {
                user.setStatus(status);
                user.setUpdateTime(java.time.LocalDateTime.now());
                userMapper.updateById(user);
            }
        }
        log.info("Admin batch updated user status: count={}, status={}", ids.size(), status);
        return Result.ok();
    }
}
