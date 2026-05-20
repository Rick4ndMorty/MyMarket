package com.tradestation.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradestation.product.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * Deduct stock with optimistic lock.
     * Returns affected rows. 0 means either stock insufficient or version conflict.
     */
    @Update("UPDATE t_inventory SET stock = stock - #{quantity}, locked_stock = locked_stock + #{quantity}, " +
            "version = version + 1 WHERE sku_id = #{skuId} AND stock >= #{quantity} AND version = #{version}")
    int deduct(@Param("skuId") Long skuId, @Param("quantity") int quantity, @Param("version") int version);

    /**
     * Restore previously deducted stock.
     */
    @Update("UPDATE t_inventory SET stock = stock + #{quantity}, locked_stock = locked_stock - #{quantity}, " +
            "version = version + 1 WHERE sku_id = #{skuId}")
    int restore(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}
