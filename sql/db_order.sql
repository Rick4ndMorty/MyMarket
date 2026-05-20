-- ============================================================
-- db_order: 订单服务数据库
-- ============================================================

CREATE DATABASE IF NOT EXISTS db_order
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE db_order;

-- 订单表
CREATE TABLE t_order (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    order_no          VARCHAR(32)     NOT NULL COMMENT '订单编号',
    user_id           BIGINT          NOT NULL COMMENT '买家用户ID',
    shop_id           BIGINT          NOT NULL COMMENT '店铺ID',
    total_amount      DECIMAL(10,2)   NOT NULL COMMENT '订单总金额',
    pay_amount        DECIMAL(10,2)   NOT NULL COMMENT '实付金额',
    address_snapshot  JSON            NOT NULL COMMENT '收货地址快照',
    status            VARCHAR(32)     NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '状态: PENDING_PAYMENT/PENDING_SHIP/SHIPPED/COMPLETED/CANCELLED',
    cancel_reason     VARCHAR(512)    DEFAULT NULL COMMENT '取消原因',
    pay_time          DATETIME        DEFAULT NULL COMMENT '支付时间',
    ship_time         DATETIME        DEFAULT NULL COMMENT '发货时间',
    complete_time     DATETIME        DEFAULT NULL COMMENT '完成时间',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_shop_id (shop_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单项表
CREATE TABLE t_order_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(32)     NOT NULL COMMENT '订单编号',
    sku_id          BIGINT          NOT NULL COMMENT 'SKU ID',
    sku_snapshot    JSON            NOT NULL COMMENT 'SKU快照(下单时的价格/规格等)',
    quantity        INT             NOT NULL COMMENT '购买数量',
    unit_price      DECIMAL(10,2)   NOT NULL COMMENT '单价',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

-- 订单操作日志表
CREATE TABLE t_order_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(32)     NOT NULL COMMENT '订单编号',
    from_status     VARCHAR(32)     DEFAULT NULL COMMENT '原状态',
    to_status       VARCHAR(32)     NOT NULL COMMENT '新状态',
    operator        VARCHAR(64)     DEFAULT NULL COMMENT '操作人',
    remark          VARCHAR(512)    DEFAULT NULL COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单操作日志表';

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
-- 测试数据: 20 个订单
-- 状态: PENDING_PAYMENT(5), PENDING_SHIP(5), SHIPPED(4), COMPLETED(4), CANCELLED(2)
-- ============================================================

INSERT INTO t_order (order_no, user_id, shop_id, total_amount, pay_amount, address_snapshot, status, cancel_reason, pay_time, ship_time, complete_time, create_time) VALUES
('202605100001', 1,  1,  208.00, 208.00, '{"receiverName":"张三","receiverPhone":"13800010001","province":"广东省","city":"深圳市","district":"南山区","detail":"科技园路1号创新大厦A座1201"}', 'COMPLETED',       NULL,    '2026-05-01 10:30:00', '2026-05-01 15:00:00', '2026-05-05 09:00:00', '2026-05-01 10:00:00'),
('202605100002', 2,  2,  398.00, 398.00, '{"receiverName":"李四","receiverPhone":"13800010002","province":"北京市","city":"北京市","district":"朝阳区","detail":"望京街道阜通东大街6号院3号楼"}',   'COMPLETED',       NULL,    '2026-05-01 14:20:00', '2026-05-02 10:00:00', '2026-05-07 12:00:00', '2026-05-01 14:00:00'),
('202605100003', 3,  3,  1250.00, 1250.00,'{"receiverName":"王五","receiverPhone":"13800010003","province":"上海市","city":"上海市","district":"浦东新区","detail":"张江高科技园区祖冲之路1500号"}','COMPLETED',       NULL,    '2026-05-02 09:00:00', '2026-05-02 16:30:00', '2026-05-06 18:00:00', '2026-05-02 08:30:00'),
('202605100004', 4,  5,  178.00, 178.00, '{"receiverName":"赵六","receiverPhone":"13800010004","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文三路478号华星时代广场B座"}',     'COMPLETED',       NULL,    '2026-05-03 11:00:00', '2026-05-03 17:00:00', '2026-05-08 10:00:00', '2026-05-03 10:30:00'),
('202605100005', 1,  3,  170.00, 170.00, '{"receiverName":"张三","receiverPhone":"13800010001","province":"广东省","city":"深圳市","district":"南山区","detail":"科技园路1号创新大厦A座1201"}',   'SHIPPED',         NULL,    '2026-05-08 16:00:00', '2026-05-09 09:30:00', NULL,                 '2026-05-08 15:30:00'),
('202605100006', 5,  5,  89.00,  89.00,  '{"receiverName":"孙七","receiverPhone":"13800010005","province":"广东省","city":"广州市","district":"天河区","detail":"天河路385号太古汇一座1802"}',   'SHIPPED',         NULL,    '2026-05-09 10:00:00', '2026-05-09 14:00:00', NULL,                 '2026-05-09 09:30:00'),
('202605100007', 6,  11, 199.00, 199.00, '{"receiverName":"周八","receiverPhone":"13800010006","province":"四川省","city":"成都市","district":"高新区","detail":"天府大道中段688号紫光大楼"}',   'SHIPPED',         NULL,    '2026-05-09 15:00:00', '2026-05-10 11:00:00', NULL,                 '2026-05-09 14:30:00'),
('202605100008', 7,  4,  99.00,  99.00,  '{"receiverName":"吴九","receiverPhone":"13800010007","province":"湖北省","city":"武汉市","district":"洪山区","detail":"珞喻路1037号华中科技大学"}',   'SHIPPED',         NULL,    '2026-05-10 09:00:00', '2026-05-10 16:00:00', NULL,                 '2026-05-10 08:30:00'),
('202605100009', 2,  2,  199.00, 199.00, '{"receiverName":"李四","receiverPhone":"13800010002","province":"北京市","city":"北京市","district":"朝阳区","detail":"望京街道阜通东大街6号院3号楼"}',   'PENDING_SHIP',    NULL,    '2026-05-10 14:00:00', NULL,                 NULL,                 '2026-05-10 13:30:00'),
('202605100010', 8,  10, 69.00,  69.00,  '{"receiverName":"郑十","receiverPhone":"13800010008","province":"江苏省","city":"南京市","district":"玄武区","detail":"中山东路301号南京图书馆旁小区2栋"}','PENDING_SHIP',    NULL,    '2026-05-10 16:00:00', NULL,                 NULL,                 '2026-05-10 15:30:00'),
('202605100011', 9,  12, 39.00,  39.00,  '{"receiverName":"钱十一","receiverPhone":"13800010009","province":"陕西省","city":"西安市","district":"雁塔区","detail":"锦业路1号都市之门C座508"}',        'PENDING_SHIP',    NULL,    '2026-05-10 17:00:00', NULL,                 NULL,                 '2026-05-10 16:30:00'),
('202605100012', 3,  14, 1299.00,1299.00,'{"receiverName":"王五","receiverPhone":"13800010003","province":"上海市","city":"上海市","district":"浦东新区","detail":"张江高科技园区祖冲之路1500号"}','PENDING_SHIP',    NULL,    '2026-05-11 08:00:00', NULL,                 NULL,                 '2026-05-11 07:30:00'),
('202605100013', 4,  9,  39.00,  39.00,  '{"receiverName":"赵六","receiverPhone":"13800010004","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文三路478号华星时代广场B座"}',     'PENDING_SHIP',    NULL,    '2026-05-11 09:00:00', NULL,                 NULL,                 '2026-05-11 08:30:00'),
('202605100014', 1,  1,  49.00,  0.00,   '{"receiverName":"张三","receiverPhone":"13800010001","province":"广东省","city":"深圳市","district":"南山区","detail":"科技园路1号创新大厦A座1201"}',   'PENDING_PAYMENT', NULL,    NULL,                 NULL,                 NULL,                 '2026-05-11 10:00:00'),
('202605100015', 7,  7,  69.00,  0.00,   '{"receiverName":"吴九","receiverPhone":"13800010007","province":"湖北省","city":"武汉市","district":"洪山区","detail":"珞喻路1037号华中科技大学"}',   'PENDING_PAYMENT', NULL,    NULL,                 NULL,                 NULL,                 '2026-05-11 10:30:00'),
('202605100016', 5,  5,  148.00, 0.00,   '{"receiverName":"孙七","receiverPhone":"13800010005","province":"广东省","city":"广州市","district":"天河区","detail":"天河路385号太古汇一座1802"}',   'PENDING_PAYMENT', NULL,    NULL,                 NULL,                 NULL,                 '2026-05-11 11:00:00'),
('202605100017', 6,  6,  388.00, 0.00,   '{"receiverName":"周八","receiverPhone":"13800010006","province":"四川省","city":"成都市","district":"高新区","detail":"天府大道中段688号紫光大楼"}',   'PENDING_PAYMENT', NULL,    NULL,                 NULL,                 NULL,                 '2026-05-11 11:30:00'),
('202605100018', 10, 12, 39.00,  0.00,   '{"receiverName":"陈十二","receiverPhone":"13800010010","province":"湖南省","city":"长沙市","district":"岳麓区","detail":"麓谷大道658号麓谷信息港A栋"}','PENDING_PAYMENT', NULL,    NULL,                 NULL,                 NULL,                 '2026-05-11 12:00:00'),
('202605100019', 4,  10, 69.00,  0.00,   '{"receiverName":"赵六","receiverPhone":"13800010004","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文三路478号华星时代广场B座"}',     'CANCELLED',       '不想要了',   NULL,              NULL,              NULL,                 '2026-05-10 09:00:00'),
('202605100020', 8,  8,  68.00,  0.00,   '{"receiverName":"郑十","receiverPhone":"13800010008","province":"江苏省","city":"南京市","district":"玄武区","detail":"中山东路301号南京图书馆旁小区2栋"}','CANCELLED',       '地址填写错误', NULL,              NULL,              NULL,                 '2026-05-11 08:00:00');

-- ============================================================
-- 测试数据: 20 个订单项
-- ============================================================

INSERT INTO t_order_item (order_id, order_no, sku_id, sku_snapshot, quantity, unit_price) VALUES
(1,  '202605100001', 1,  '{"skuName":"iPhone 16 Pro Max 黑色","price":49.00,"image":"https://picsum.photos/seed/iphone-case-black/400/400"}', 2, 49.00),
(1,  '202605100001', 4,  '{"skuName":"Anker 65W 白色","price":159.00,"image":"https://picsum.photos/seed/gan-white/400/400"}', 1, 159.00),
(2,  '202605100002', 6,  '{"skuName":"碎花连衣裙 S码","price":199.00,"image":"https://picsum.photos/seed/dress-s/400/400"}', 2, 199.00),
(3,  '202605100003', 11, '{"skuName":"小黑瓶精华液 50ml","price":1080.00,"image":"https://picsum.photos/seed/serum-sku/400/400"}', 1, 1080.00),
(3,  '202605100003', 12, '{"skuName":"Chili 小辣椒","price":170.00,"image":"https://picsum.photos/seed/lipstick-chili/400/400"}', 1, 170.00),
(4,  '202605100004', 16, '{"skuName":"手打牛肉丸 纯牛肉","price":89.00,"image":"https://picsum.photos/seed/beef-original/400/400"}', 2, 89.00),
(5,  '202605100005', 12, '{"skuName":"Chili 小辣椒","price":170.00,"image":"https://picsum.photos/seed/lipstick-chili/400/400"}', 1, 170.00),
(6,  '202605100006', 16, '{"skuName":"手打牛肉丸 纯牛肉","price":89.00,"image":"https://picsum.photos/seed/beef-original/400/400"}', 1, 89.00),
(7,  '202605100007', 3,  '{"skuName":"碎花连衣裙 S码","price":199.00,"image":"https://picsum.photos/seed/dress-s/400/400"}', 1, 199.00),
(8,  '202605100008', 14, '{"skuName":"三体全集典藏版","price":99.00,"image":"https://picsum.photos/seed/threebody-sku/400/400"}', 1, 99.00),
(9,  '202605100009', 6,  '{"skuName":"碎花连衣裙 S码","price":199.00,"image":"https://picsum.photos/seed/dress-s/400/400"}', 1, 199.00),
(10, '202605100010', 26, '{"skuName":"旋转收纳盘 白色双层","price":69.00,"image":"https://picsum.photos/seed/storage-white/400/400"}', 1, 69.00),
(11, '202605100011', 21, '{"skuName":"头水紫菜 3包装","price":39.00,"image":"https://picsum.photos/seed/seaweed-sku/400/400"}', 1, 39.00),
(12, '202605100012', 23, '{"skuName":"米家扫拖机器人3C","price":1299.00,"image":"https://picsum.photos/seed/robot3c-sku/400/400"}', 1, 1299.00),
(13, '202605100013', 24, '{"skuName":"特辣火锅底料 500g","price":39.00,"image":"https://picsum.photos/seed/hotpot-spicy/400/400"}', 1, 39.00),
(14, '202605100014', 1,  '{"skuName":"iPhone 16 Pro Max 黑色","price":49.00,"image":"https://picsum.photos/seed/iphone-case-black/400/400"}', 1, 49.00),
(15, '202605100015', 22, '{"skuName":"手工鱼丸 2包装","price":69.00,"image":"https://picsum.photos/seed/fishball-sku/400/400"}', 1, 69.00),
(16, '202605100016', 16, '{"skuName":"手打牛肉丸 纯牛肉","price":89.00,"image":"https://picsum.photos/seed/beef-original/400/400"}', 1, 89.00),
(16, '202605100016', 18, '{"skuName":"虾蟹粥料包","price":59.00,"image":"https://picsum.photos/seed/congee-crab/400/400"}', 1, 59.00),
(17, '202605100017', 19, '{"skuName":"碧螺春 250g 礼盒装","price":388.00,"image":"https://picsum.photos/seed/biluochun-sku/400/400"}', 1, 388.00),
(18, '202605100018', 21, '{"skuName":"头水紫菜 3包装","price":39.00,"image":"https://picsum.photos/seed/seaweed-sku/400/400"}', 1, 39.00),
(19, '202605100019', 26, '{"skuName":"旋转收纳盘 白色双层","price":69.00,"image":"https://picsum.photos/seed/storage-white/400/400"}', 1, 69.00),
(20, '202605100020', 15, '{"skuName":"原则 精装版","price":68.00,"image":"https://picsum.photos/seed/principles-sku/400/400"}', 1, 68.00);

-- ============================================================
-- 测试数据: 20 条订单操作日志
-- ============================================================

INSERT INTO t_order_log (order_id, order_no, from_status, to_status, operator, remark) VALUES
(1,  '202605100001', NULL,              'PENDING_PAYMENT',  'buyer01',  '买家下单'),
(1,  '202605100001', 'PENDING_PAYMENT',  'PENDING_SHIP',    'system',   '支付成功，等待发货'),
(1,  '202605100001', 'PENDING_SHIP',     'SHIPPED',         'seller01', '已发货，顺丰快递 SF1234567890'),
(1,  '202605100001', 'SHIPPED',          'COMPLETED',       'system',   '买家确认收货，订单完成'),
(2,  '202605100002', NULL,               'PENDING_PAYMENT', 'buyer02',  '买家下单'),
(2,  '202605100002', 'PENDING_PAYMENT',   'PENDING_SHIP',   'system',   '支付成功，等待发货'),
(2,  '202605100002', 'PENDING_SHIP',      'SHIPPED',        'seller02', '已发货，中通快递 ZT0987654321'),
(2,  '202605100002', 'SHIPPED',           'COMPLETED',      'system',   '买家确认收货，订单完成'),
(5,  '202605100005', NULL,               'PENDING_PAYMENT', 'buyer01',  '买家下单'),
(5,  '202605100005', 'PENDING_PAYMENT',   'PENDING_SHIP',   'system',   '支付成功，等待发货'),
(5,  '202605100005', 'PENDING_SHIP',      'SHIPPED',        'seller03', '已发货，圆通快递 YT1122334455'),
(12, '202605100012', NULL,               'PENDING_PAYMENT', 'buyer03',  '买家下单'),
(12, '202605100012', 'PENDING_PAYMENT',   'PENDING_SHIP',   'system',   '支付成功，等待发货'),
(14, '202605100014', NULL,               'PENDING_PAYMENT', 'buyer01',  '买家下单'),
(15, '202605100015', NULL,               'PENDING_PAYMENT', 'buyer07',  '买家下单'),
(16, '202605100016', NULL,               'PENDING_PAYMENT', 'buyer05',  '买家下单'),
(17, '202605100017', NULL,               'PENDING_PAYMENT', 'buyer06',  '买家下单'),
(18, '202605100018', NULL,               'PENDING_PAYMENT', 'buyer10',  '买家下单'),
(19, '202605100019', NULL,               'PENDING_PAYMENT', 'buyer04',  '买家下单'),
(19, '202605100019', 'PENDING_PAYMENT',   'CANCELLED',      'buyer04',  '买家取消订单，原因: 不想要了'),
(20, '202605100020', NULL,               'PENDING_PAYMENT', 'buyer08',  '买家下单'),
(20, '202605100020', 'PENDING_PAYMENT',   'CANCELLED',      'buyer08',  '买家取消订单，原因: 地址填写错误');
