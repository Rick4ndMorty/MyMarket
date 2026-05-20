package com.tradestation.user.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.common.util.JwtUtil;
import com.tradestation.user.entity.User;
import com.tradestation.user.mapper.UserMapper;
import com.tradestation.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    @SentinelResource(value = "register", blockHandler = "registerBlockHandler")
    public Result<Void> register(String username, String password, String email, String phone, String role) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, username);
        if (userMapper.selectCount(query) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role != null ? role : "user");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        log.info("user registered: userId={}, username={}", user.getId(), username);
        return Result.ok();
    }

    @Override
    @SentinelResource(value = "login", blockHandler = "loginBlockHandler")
    public Result<Map<String, Object>> login(String username, String password) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, username);
        User user = userMapper.selectOne(query);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);

        log.info("user login: userId={}, username={}", user.getId(), username);
        return Result.ok(result);
    }

    @Override
    public Result<Map<String, Object>> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("avatar", user.getAvatar());
        profile.put("role", user.getRole());
        profile.put("status", user.getStatus());
        profile.put("createTime", user.getCreateTime());

        return Result.ok(profile);
    }

    @Override
    public Result<Void> updateProfile(Long userId, String email, String phone, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (email != null) {
            user.setEmail(email);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        user.setUpdateTime(LocalDateTime.now());

        userMapper.updateById(user);
        log.info("profile updated: userId={}", userId);
        return Result.ok();
    }

    @Override
    public Result<Void> updateRole(Long userId, String role) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setRole(role);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("user role updated: userId={}, role={}", userId, role);
        return Result.ok();
    }

    // ---- Sentinel block handlers ----

    public Result<Void> registerBlockHandler(String username, String password, String email, String phone, String role, BlockException e) {
        log.error("(触发限流)register blocked by Sentinel: username={}", username, e);
        return Result.fail(ErrorCode.RATE_LIMITED);
    }

    public Result<Map<String, Object>> loginBlockHandler(String username, String password, BlockException e) {
        log.error("(触发限流)login blocked by Sentinel: username={}", username, e);
        return Result.fail(ErrorCode.RATE_LIMITED);
    }
}
