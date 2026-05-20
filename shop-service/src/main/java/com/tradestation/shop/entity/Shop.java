package com.tradestation.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_shop")
public class Shop {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String shopName;

    private String logo;

    private String description;

    private String phone;

    private String status;

    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
