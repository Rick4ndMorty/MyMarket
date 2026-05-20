package com.tradestation.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradestation.common.enums.ErrorCode;
import com.tradestation.common.exception.BusinessException;
import com.tradestation.common.result.Result;
import com.tradestation.user.entity.UserAddress;
import com.tradestation.user.mapper.UserAddressMapper;
import com.tradestation.user.service.AddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AddressServiceImpl implements AddressService {

    private final UserAddressMapper addressMapper;

    public AddressServiceImpl(UserAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public Result<List<UserAddress>> list(Long userId) {
        LambdaQueryWrapper<UserAddress> query = new LambdaQueryWrapper<>();
        query.eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getCreateTime);
        List<UserAddress> list = addressMapper.selectList(query);
        return Result.ok(list);
    }

    @Override
    public Result<UserAddress> add(Long userId, UserAddress addr) {
        addr.setUserId(userId);
        addr.setCreateTime(LocalDateTime.now());
        addr.setUpdateTime(LocalDateTime.now());
        addressMapper.insert(addr);
        log.info("address added: userId={}, addressId={}", userId, addr.getId());
        return Result.ok(addr);
    }

    @Override
    public Result<Void> update(Long userId, Long id, UserAddress addr) {
        UserAddress existing = addressMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        addr.setId(id);
        addr.setUserId(userId);
        addr.setUpdateTime(LocalDateTime.now());
        addressMapper.updateById(addr);
        log.info("address updated: userId={}, addressId={}", userId, id);
        return Result.ok();
    }

    @Override
    public Result<Void> delete(Long userId, Long id) {
        UserAddress existing = addressMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        addressMapper.deleteById(id);
        log.info("address deleted: userId={}, addressId={}", userId, id);
        return Result.ok();
    }

    @Override
    public Result<UserAddress> getById(Long id) {
        UserAddress addr = addressMapper.selectById(id);
        if (addr == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return Result.ok(addr);
    }
}
