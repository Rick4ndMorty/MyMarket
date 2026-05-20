package com.tradestation.user.service;

import com.tradestation.common.result.Result;
import com.tradestation.user.entity.UserAddress;

import java.util.List;

public interface AddressService {

    Result<List<UserAddress>> list(Long userId);

    Result<UserAddress> add(Long userId, UserAddress addr);

    Result<Void> update(Long userId, Long id, UserAddress addr);

    Result<Void> delete(Long userId, Long id);

    Result<UserAddress> getById(Long id);
}
