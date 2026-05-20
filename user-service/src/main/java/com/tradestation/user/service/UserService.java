package com.tradestation.user.service;

import com.tradestation.common.result.Result;

import java.util.Map;

public interface UserService {

    Result<Void> register(String username, String password, String email, String phone, String role);

    Result<Map<String, Object>> login(String username, String password);

    Result<Map<String, Object>> getProfile(Long userId);

    Result<Void> updateProfile(Long userId, String email, String phone, String avatar);

    Result<Void> updateRole(Long userId, String role);
}
