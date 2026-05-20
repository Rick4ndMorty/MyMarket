package com.tradestation.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_customer_message")
public class CustomerMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private Long userId;

    private String senderType;

    private String messageType;

    private String content;

    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
