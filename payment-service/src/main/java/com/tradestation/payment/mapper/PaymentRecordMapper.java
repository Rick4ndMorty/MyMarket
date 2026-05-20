package com.tradestation.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradestation.payment.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
