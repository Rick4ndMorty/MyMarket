package com.tradestation.user.controller;

import com.tradestation.common.result.Result;
import com.tradestation.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> body) {
        return userService.register(
                body.get("username"),
                body.get("password"),
                body.get("email"),
                body.get("phone"),
                body.get("role"));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return userService.login(body.get("username"), body.get("password"));
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile(@RequestHeader("X-User-Id") Long userId) {
        return userService.getProfile(userId);
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                       @RequestBody Map<String, String> body) {
        return userService.updateProfile(
                userId,
                body.get("email"),
                body.get("phone"),
                body.get("avatar"));
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return userService.updateRole(id, body.get("role"));
    }

    @GetMapping("/inner/{id}")
    public Result<Map<String, Object>> getUserById(@PathVariable Long id) {
        return userService.getProfile(id);
    }
}
