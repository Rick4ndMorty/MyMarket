-- ============================================================
-- db_shop: 店铺服务数据库
-- ============================================================

CREATE DATABASE IF NOT EXISTS db_shop
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE db_shop;

-- 店铺表
CREATE TABLE t_shop (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
    user_id         BIGINT          NOT NULL COMMENT '店主用户ID',
    shop_name       VARCHAR(128)    NOT NULL COMMENT '店铺名称',
    logo            VARCHAR(512)    DEFAULT NULL COMMENT '店铺Logo URL',
    description     TEXT            DEFAULT NULL COMMENT '店铺简介',
    phone           VARCHAR(32)     DEFAULT NULL COMMENT '店铺联系电话',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/ACTIVE/CLOSED/REJECTED',
    reject_reason   VARCHAR(512)    DEFAULT NULL COMMENT '审核拒绝原因',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺表';

-- 店铺员工表
CREATE TABLE t_shop_staff (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '员工ID',
    shop_id         BIGINT          NOT NULL COMMENT '店铺ID',
    user_id         BIGINT          NOT NULL COMMENT '员工用户ID',
    role            VARCHAR(16)     NOT NULL DEFAULT 'STAFF' COMMENT '角色: STAFF/ADMIN',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_user (shop_id, user_id),
    KEY idx_shop_id (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺员工表';

-- 客服消息表 (预留 AI 客服字段)
CREATE TABLE t_customer_message (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    shop_id         BIGINT          NOT NULL COMMENT '店铺ID',
    user_id         BIGINT          NOT NULL COMMENT '买家用户ID',
    sender_type     VARCHAR(16)     NOT NULL COMMENT '发送人类型: BUYER/SELLER/AI',
    message_type    VARCHAR(16)     NOT NULL COMMENT '消息类型: CUSTOMER(人工)/AI(AI客服)',
    content         TEXT            NOT NULL COMMENT '消息内容',
    is_read         TINYINT         NOT NULL DEFAULT 0 COMMENT '是否已读: 1是 0否',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_shop_user (shop_id, user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客服消息表(预留AI客服)';

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
-- 测试数据: 20 个店铺 (ACTIVE 12, PENDING 3, CLOSED 3, REJECTED 2)
-- ============================================================

INSERT INTO t_shop (user_id, shop_name, logo, description, phone, status, reject_reason) VALUES
(11, '极客数码旗舰店',   'https://picsum.photos/seed/shop-logo-01/200/200',  '正品数码产品，手机电脑配件一应俱全，品质保证，售后无忧。',    '0755-86010001', 'ACTIVE',   NULL),
(12, '江南布衣坊',       'https://picsum.photos/seed/shop-logo-02/200/200',  '原创设计女装，棉麻天然面料，让每一位女性穿出东方韵味。',      '0571-88020001', 'ACTIVE',   NULL),
(13, '海淘美妆店',       'https://picsum.photos/seed/shop-logo-03/200/200',  '海外直邮美妆护肤正品，日韩欧美大牌授权经销商。',               '021-64030001', 'ACTIVE',   NULL),
(14, '京城书店',         'https://picsum.photos/seed/shop-logo-04/200/200',  '文学、社科、经管、童书，定期举办读书会，让阅读成为一种生活方式。', '010-82040001', 'ACTIVE',   NULL),
(15, '潮汕美食坊',       'https://picsum.photos/seed/shop-logo-05/200/200',  '正宗潮汕手打牛肉丸、砂锅粥、卤水，冷链直达，鲜到家。',        '020-84050001', 'ACTIVE',   NULL),
(16, '苏式生活馆',       'https://picsum.photos/seed/shop-logo-06/200/200',  '苏州特产、丝绸制品、碧螺春茶叶，一份来自江南的礼物。',        '0512-66060001', 'ACTIVE',   NULL),
(17, '闽南海味铺',       'https://picsum.photos/seed/shop-logo-07/200/200',  '厦门直供海鲜干货、紫菜、鱼丸，闽南古早味代代相传。',         '0592-66070001', 'ACTIVE',   NULL),
(18, '智能家居体验馆',   'https://picsum.photos/seed/shop-logo-08/200/200',  '智能音箱、扫地机器人、智能灯控，打造未来家居生活。',          '0755-86080001', 'ACTIVE',   NULL),
(19, '山城火锅底料专营', 'https://picsum.photos/seed/shop-logo-09/200/200',  '重庆老火锅底料、麻辣调料、火锅食材一站式采购。',              '023-67090001', 'ACTIVE',   NULL),
(1,  '生活家杂货铺',     'https://picsum.photos/seed/shop-logo-10/200/200',  '日用百货、收纳整理、厨房神器，让生活更便捷舒适。',            '0755-86100001', 'ACTIVE',   NULL),
(2,  '乐活运动户外',     'https://picsum.photos/seed/shop-logo-11/200/200',  '跑步、登山、瑜伽装备，专业品牌代理，开启健康生活。',         '010-82110001', 'ACTIVE',   NULL),
(3,  '萌宠乐园',         'https://picsum.photos/seed/shop-logo-12/200/200',  '猫粮狗粮、宠物玩具、美容用品，让毛孩子过得舒适又快乐。',      '021-64120001', 'ACTIVE',   NULL),
(4,  '待审核家居店',     'https://picsum.photos/seed/shop-logo-13/200/200',  '精品家居装饰，轻奢风格灯具与摆件，提升家居品味。',            NULL,          'PENDING',  NULL),
(5,  '待审核零食铺',     'https://picsum.photos/seed/shop-logo-14/200/200',  '进口零食、网红食品、办公室零食大礼包。',                      NULL,          'PENDING',  NULL),
(6,  '待审核手工艺品',   'https://picsum.photos/seed/shop-logo-15/200/200',  '手工皮具、木艺、陶艺，每一件都是独一无二的匠心之作。',        NULL,          'PENDING',  NULL),
(7,  '已关闭数码维修',   'https://picsum.photos/seed/shop-logo-16/200/200',  '手机维修、电脑维修、数据恢复，原厂配件，专业可靠。',          '0755-86160001', 'CLOSED',   '店主主动申请关闭'),
(8,  '已关闭服饰店',     'https://picsum.photos/seed/shop-logo-17/200/200',  '快时尚男装女装，每周上新，潮流不贵。',                        '0571-88170001', 'CLOSED',   '违规经营'),
(9,  '已关闭零食店',     'https://picsum.photos/seed/shop-logo-18/200/200',  '坚果炒货、果脯蜜饯、地方特产小吃，好吃不贵。',                '020-84180001', 'CLOSED',   '店主主动申请关闭'),
(10, '被拒母婴店',       'https://picsum.photos/seed/shop-logo-19/200/200',  '进口母婴用品，奶粉纸尿裤婴儿辅食，安全放心。',                NULL,          'REJECTED', '营业执照信息不符'),
(20, '被拒电子烟店',     'https://picsum.photos/seed/shop-logo-20/200/200',  '电子烟及配件专卖。',                                          NULL,          'REJECTED', '类目不符合平台规定');

-- ============================================================
-- 测试数据: 10 个店铺员工
-- ============================================================

INSERT INTO t_shop_staff (shop_id, user_id, role) VALUES
(1,  12, 'STAFF'),
(1,  13, 'STAFF'),
(2,  14, 'ADMIN'),
(3,  15, 'STAFF'),
(4,  16, 'STAFF'),
(5,  17, 'ADMIN'),
(5,  1,  'STAFF'),
(6,  2,  'STAFF'),
(10, 3,  'STAFF'),
(12, 4,  'STAFF');

-- ============================================================
-- 测试数据: 20 条客服消息 (BUYER/SELLER/AI 发送)
-- ============================================================

INSERT INTO t_customer_message (shop_id, user_id, sender_type, message_type, content, is_read) VALUES
(1,  1,  'BUYER',  'CUSTOMER', '你好，请问这款手机壳支持 iPhone 15 吗？',           1),
(1,  11, 'SELLER', 'CUSTOMER', '亲，支持的哦，iPhone 15 全系列都有对应型号。',        1),
(2,  2,  'BUYER',  'CUSTOMER', '这件连衣裙有XL码吗？',                               1),
(2,  12, 'SELLER', 'CUSTOMER', '您好，这款暂时最大L码，可以看下店里其他宽松版型。',     1),
(3,  3,  'BUYER',  'CUSTOMER', '这个精华液敏感肌能用吗？',                            0),
(5,  4,  'BUYER',  'CUSTOMER', '牛肉丸是当天现做的吗？',                             1),
(5,  15, 'SELLER', 'CUSTOMER', '是的，每天早上手工打制，冷链发出，保证新鲜。',          1),
(10, 5,  'BUYER',  'CUSTOMER', '这个收纳盒尺寸是多少？',                             0),
(11, 6,  'BUYER',  'CUSTOMER', '这个跑步鞋偏码吗？',                                 1),
(11, 2,  'SELLER', 'CUSTOMER', '您好，这款正常码，脚宽建议买大半码哦。',               1),
(12, 7,  'BUYER',  'CUSTOMER', '这款猫粮适合布偶猫吃吗？',                           0),
(1,  8,  'BUYER',  'CUSTOMER', '请问有实体店可以自提吗？',                           0),
(2,  9,  'BUYER',  'CUSTOMER', '支持七天无理由退货吗？',                             1),
(2,  12, 'SELLER', 'CUSTOMER', '支持的呢，不影响二次销售的话随时可以退换。',            1),
(3,  10, 'BUYER',  'CUSTOMER', '发货后一般几天能到上海？',                           0),
(14, 9,  'BUYER',  'CUSTOMER', '你好，想问下这款手链是纯银的吗？',                    0),
(14, 9,  'AI',     'AI',       '您好！客服暂时不在线，我是AI助手，已记录您的问题，店主稍后会回复您。', 0),
(5,  18, 'BUYER',  'CUSTOMER', '能不能发顺丰到付？',                                 0),
(5,  18, 'AI',     'AI',       '您好，客服当前繁忙，我是AI助手小美，店铺支持顺丰到付，下单时备注即可。', 0),
(7,  1,  'BUYER',  'CUSTOMER', '鱼丸能不能真空包装？想寄给外地的朋友。',                0);
