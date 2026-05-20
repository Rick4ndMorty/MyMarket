package com.tradestation.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradestation.order.entity.OrderLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderLogMapper extends BaseMapper<OrderLog> {
}
