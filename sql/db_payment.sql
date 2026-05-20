-- ============================================================
-- db_payment: 支付服务数据库
-- ============================================================

CREATE DATABASE IF NOT EXISTS db_payment
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE db_payment;

-- 支付记录表
CREATE TABLE t_payment_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
    payment_no      VARCHAR(32)     NOT NULL COMMENT '支付单号',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(32)     NOT NULL COMMENT '订单编号',
    user_id         BIGINT          NOT NULL COMMENT '付款用户ID',
    amount          DECIMAL(10,2)   NOT NULL COMMENT '支付金额',
    payment_method  VARCHAR(32)     NOT NULL DEFAULT 'MOCK' COMMENT '支付方式: MOCK(模拟)/ALIPAY/WECHAT',
    status          VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/SUCCESS/FAILED/REFUNDED',
    callback_data   JSON            DEFAULT NULL COMMENT '支付回调原始数据',
    pay_time        DATETIME        DEFAULT NULL COMMENT '支付成功时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- Seata AT 模式 undo_log 表
CREATE TABLE undo_log (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    branch_id     BIGINT          NOT NULL,
    xid           VARCHAR(128)    NOT NULL,
    context       VARCHAR(128)    NOT NULL,
    rollback_info LONGBLOB        NOT NULL,
    log_status    INT             NOT NULL,
    log_created   DATETIME        NOT NULL,
    log_modified  DATETIME        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seata undo_log';

-- ============================================================
-- 测试数据: 20 条支付记录
-- 状态: SUCCESS(12), PENDING(5), FAILED(1), REFUNDED(2)
-- 支付方式: MOCK(12), ALIPAY(5), WECHAT(3)
-- ============================================================

INSERT INTO t_payment_record (payment_no, order_id, order_no, user_id, amount, payment_method, status, callback_data, pay_time, create_time) VALUES
('PAY20260501001', 1,  '202605100001', 1,  257.00,  'ALIPAY', 'SUCCESS',  '{"trade_no":"2026050122001412345678901234","buyer_id":"2088001234567890"}', '2026-05-01 10:30:00', '2026-05-01 10:00:00'),
('PAY20260501002', 2,  '202605100002', 2,  398.00,  'WECHAT', 'SUCCESS',  '{"transaction_id":"4200001234567890123456789012","openid":"oTest1234567890"}', '2026-05-01 14:20:00', '2026-05-01 14:00:00'),
('PAY20260502001', 3,  '202605100003', 3,  1250.00, 'ALIPAY', 'SUCCESS',  '{"trade_no":"2026050222001412345678901235","buyer_id":"2088001234567891"}', '2026-05-02 09:00:00', '2026-05-02 08:30:00'),
('PAY20260503001', 4,  '202605100004', 4,  178.00,  'WECHAT', 'SUCCESS',  '{"transaction_id":"4200001234567890123456789013","openid":"oTest1234567891"}', '2026-05-03 11:00:00', '2026-05-03 10:30:00'),
('PAY20260508001', 5,  '202605100005', 1,  170.00,  'MOCK',   'SUCCESS',  '{"mock":true}',                                                     '2026-05-08 16:00:00', '2026-05-08 15:30:00'),
('PAY20260509001', 6,  '202605100006', 5,  89.00,   'MOCK',   'SUCCESS',  '{"mock":true}',                                                     '2026-05-09 10:00:00', '2026-05-09 09:30:00'),
('PAY20260509002', 7,  '202605100007', 6,  199.00,  'MOCK',   'SUCCESS',  '{"mock":true}',                                                     '2026-05-09 15:00:00', '2026-05-09 14:30:00'),
('PAY20260510001', 8,  '202605100008', 7,  99.00,   'MOCK',   'SUCCESS',  '{"mock":true}',                                                     '2026-05-10 09:00:00', '2026-05-10 08:30:00'),
('PAY20260510002', 9,  '202605100009', 2,  199.00,  'MOCK',   'SUCCESS',  '{"mock":true}',                                                     '2026-05-10 14:00:00', '2026-05-10 13:30:00'),
('PAY20260510003', 10, '202605100010', 8,  69.00,   'MOCK',   'SUCCESS',  '{"mock":true}',                                                     '2026-05-10 16:00:00', '2026-05-10 15:30:00'),
('PAY20260510004', 11, '202605100011', 9,  39.00,   'MOCK',   'SUCCESS',  '{"mock":true}',                                                     '2026-05-10 17:00:00', '2026-05-10 16:30:00'),
('PAY20260511001', 12, '202605100012', 3,  1299.00, 'ALIPAY', 'SUCCESS',  '{"trade_no":"2026051122001412345678901236","buyer_id":"2088001234567890"}', '2026-05-11 08:00:00', '2026-05-11 07:30:00'),
('PAY20260511002', 13, '202605100013', 4,  39.00,   'MOCK',   'SUCCESS',  '{"mock":true}',                                                     '2026-05-11 09:00:00', '2026-05-11 08:30:00'),
('PAY20260511003', 14, '202605100014', 1,  49.00,   'MOCK',   'PENDING',  NULL,                                                               NULL,                 '2026-05-11 10:00:00'),
('PAY20260511004', 15, '202605100015', 7,  69.00,   'MOCK',   'PENDING',  NULL,                                                               NULL,                 '2026-05-11 10:30:00'),
('PAY20260511005', 16, '202605100016', 5,  148.00,  'ALIPAY', 'PENDING',  NULL,                                                               NULL,                 '2026-05-11 11:00:00'),
('PAY20260511006', 17, '202605100017', 6,  388.00,  'WECHAT', 'PENDING',  NULL,                                                               NULL,                 '2026-05-11 11:30:00'),
('PAY20260511007', 18, '202605100018', 10, 39.00,   'MOCK',   'PENDING',  NULL,                                                               NULL,                 '2026-05-11 12:00:00'),
('PAY20260510005', 19, '202605100019', 4,  69.00,   'MOCK',   'REFUNDED', '{"refund_no":"REF20260510001","refund_amount":69.00}',            '2026-05-10 09:30:00', '2026-05-10 09:00:00'),
('PAY20260510006', 20, '202605100020', 8,  68.00,   'ALIPAY', 'FAILED',   '{"error_code":"ACCOUNT_BALANCE_INSUFFICIENT","error_msg":"账户余额不足"}', NULL,           '2026-05-11 08:00:00');
