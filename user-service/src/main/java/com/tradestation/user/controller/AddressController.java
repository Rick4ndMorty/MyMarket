package com.tradestation.user.controller;

import com.tradestation.common.result.Result;
import com.tradestation.user.entity.UserAddress;
import com.tradestation.user.service.AddressService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public Result<List<UserAddress>> list(@RequestHeader("X-User-Id") Long userId) {
        return addressService.list(userId);
    }

    @PostMapping
    public Result<UserAddress> add(@RequestHeader("X-User-Id") Long userId,
                                    @RequestBody UserAddress addr) {
        return addressService.add(userId, addr);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@RequestHeader("X-User-Id") Long userId,
                                @PathVariable Long id,
                                @RequestBody UserAddress addr) {
        return addressService.update(userId, id, addr);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("X-User-Id") Long userId,
                                @PathVariable Long id) {
        return addressService.delete(userId, id);
    }

    @GetMapping("/{id}")
    public Result<UserAddress> getById(@PathVariable Long id) {
        return addressService.getById(id);
    }
}
