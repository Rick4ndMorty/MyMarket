-- ============================================================
-- db_product: 商品服务数据库
-- ============================================================

CREATE DATABASE IF NOT EXISTS db_product
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE db_product;

-- 商品表
CREATE TABLE t_product (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    shop_id         BIGINT          NOT NULL COMMENT '店铺ID',
    product_name    VARCHAR(256)    NOT NULL COMMENT '商品名称',
    description     TEXT            DEFAULT NULL COMMENT '商品描述',
    main_image      VARCHAR(512)    DEFAULT NULL COMMENT '商品主图URL',
    images          JSON            DEFAULT NULL COMMENT '商品图片列表(JSON数组)',
    category_id     BIGINT          DEFAULT NULL COMMENT '分类ID',
    status          VARCHAR(16)     NOT NULL DEFAULT 'ON_SHELF' COMMENT '状态: ON_SHELF/OFF_SHELF/DELETED',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_shop_id (shop_id),
    KEY idx_category_id (category_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- SKU表
CREATE TABLE t_product_sku (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
    product_id      BIGINT          NOT NULL COMMENT '商品ID',
    sku_code        VARCHAR(128)    NOT NULL COMMENT 'SKU编码',
    sku_name        VARCHAR(256)    NOT NULL COMMENT 'SKU名称(规格描述)',
    specifications  JSON            DEFAULT NULL COMMENT '规格详情(JSON: {"颜色":"红","尺寸":"XL"})',
    price           DECIMAL(10,2)   NOT NULL COMMENT '售价',
    image           VARCHAR(512)    DEFAULT NULL COMMENT 'SKU图片URL',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU表';

-- 库存表 (乐观锁)
CREATE TABLE t_inventory (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    sku_id          BIGINT          NOT NULL COMMENT 'SKU ID',
    stock           INT             NOT NULL DEFAULT 0 COMMENT '可用库存',
    locked_stock    INT             NOT NULL DEFAULT 0 COMMENT '锁定库存(下单未支付)',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';

-- 商品评价/问答表
CREATE TABLE t_product_review (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    product_id      BIGINT          NOT NULL COMMENT '商品ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    order_id        BIGINT          DEFAULT NULL COMMENT '订单ID(评价关联订单)',
    type            VARCHAR(16)     NOT NULL DEFAULT 'REVIEW' COMMENT '类型: REVIEW/QUESTION/ANSWER/FOLLOWUP',
    parent_id       BIGINT          DEFAULT NULL COMMENT '父评价ID(问答回复/追评)',
    rating          INT             DEFAULT NULL COMMENT '评分1-5(仅REVIEW)',
    content         TEXT            NOT NULL COMMENT '评价内容',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_product_id (product_id),
    KEY idx_user_id (user_id),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品评价/问答表';

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
-- 测试数据: 20 个商品 (ON_SHELF 14, OFF_SHELF 4, DELETED 2)
-- 商品图片使用 picsum.photos 真实照片
-- ============================================================

INSERT INTO t_product (shop_id, product_name, description, main_image, images, category_id, status) VALUES
(1,  'iPhone 16 Pro Max 磁吸手机壳',
     '液态硅胶材质，全包防摔，MagSafe磁吸兼容，亲肤手感，多色可选。',
     'https://picsum.photos/seed/iphone-case/600/600',
     '["https://picsum.photos/seed/iphone-case-1/600/600","https://picsum.photos/seed/iphone-case-2/600/600","https://picsum.photos/seed/iphone-case-3/600/600"]',
     1, 'ON_SHELF'),

(1,  'Anker 65W 氮化镓充电器',
     '三口快充，支持手机/平板/笔记本，GaN技术，小巧便携，出差必备。',
     'https://picsum.photos/seed/gan-charger/600/600',
     '["https://picsum.photos/seed/gan-charger-1/600/600","https://picsum.photos/seed/gan-charger-2/600/600"]',
     1, 'ON_SHELF'),

(2,  '春夏季新款法式碎花连衣裙',
     'V领收腰设计，雪纺面料透气飘逸，小碎花印花，适合日常通勤和度假。',
     'https://picsum.photos/seed/floral-dress/600/600',
     '["https://picsum.photos/seed/floral-dress-1/600/600","https://picsum.photos/seed/floral-dress-2/600/600","https://picsum.photos/seed/floral-dress-3/600/600"]',
     2, 'ON_SHELF'),

(2,  '纯棉宽松休闲衬衫',
     '100%新疆长绒棉，日系简约风，男女同款，适合叠穿，四季百搭单品。',
     'https://picsum.photos/seed/cotton-shirt/600/600',
     '["https://picsum.photos/seed/cotton-shirt-1/600/600","https://picsum.photos/seed/cotton-shirt-2/600/600"]',
     2, 'ON_SHELF'),

(3,  '兰蔻小黑瓶肌底精华液 50ml',
     '二裂酵母发酵产物，修护肌底，促进后续护肤品吸收，敏感肌可用。',
     'https://picsum.photos/seed/serum/600/600',
     '["https://picsum.photos/seed/serum-1/600/600","https://picsum.photos/seed/serum-2/600/600"]',
     3, 'ON_SHELF'),

(3,  'MAC子弹头口红 Chili',
     '经典铁锈红，哑光质地，显白不挑皮，持久不脱色，送女友首选。',
     'https://picsum.photos/seed/lipstick/600/600',
     '["https://picsum.photos/seed/lipstick-1/600/600","https://picsum.photos/seed/lipstick-2/600/600"]',
     3, 'ON_SHELF'),

(4,  '《三体》全集 典藏版 (全3册)',
     '刘慈欣科幻巨著，雨果奖获奖作品，硬壳精装，附赠概念插画海报。',
     'https://picsum.photos/seed/three-body/600/600',
     '["https://picsum.photos/seed/three-body-1/600/600","https://picsum.photos/seed/three-body-2/600/600"]',
     4, 'ON_SHELF'),

(4,  '《原则》瑞·达利欧 著',
     '桥水基金创始人的生活与工作原则，投资与管理必读经典。',
     'https://picsum.photos/seed/principles-book/600/600',
     '["https://picsum.photos/seed/principles-book-1/600/600"]',
     4, 'ON_SHELF'),

(5,  '潮汕手打牛肉丸 500g*2包',
     '纯手工捶打，精选牛后腿肉，Q弹爆汁，火锅烧烤煲汤必备。',
     'https://picsum.photos/seed/beef-ball/600/600',
     '["https://picsum.photos/seed/beef-ball-1/600/600","https://picsum.photos/seed/beef-ball-2/600/600"]',
     5, 'ON_SHELF'),

(5,  '潮汕砂锅粥料包 (虾蟹组合) 3-4人份',
     '含珍珠米、瑶柱、干贝、虾仁、香菇等，傻瓜式操作，在家做出地道砂锅粥。',
     'https://picsum.photos/seed/congee-pack/600/600',
     '["https://picsum.photos/seed/congee-pack-1/600/600","https://picsum.photos/seed/congee-pack-2/600/600"]',
     5, 'ON_SHELF'),

(6,  '明前碧螺春 特级 250g 礼盒装',
     '苏州洞庭山原产地，手工炒制，白毫显露，花果香浓郁，送礼自饮皆宜。',
     'https://picsum.photos/seed/biluochun/600/600',
     '["https://picsum.photos/seed/biluochun-1/600/600","https://picsum.photos/seed/biluochun-2/600/600"]',
     6, 'ON_SHELF'),

(7,  '厦门特产 头水紫菜 100g*3包',
     '东山岛头水紫菜，无沙免洗，煲汤做汤，口感嫩滑，营养丰富。',
     'https://picsum.photos/seed/seaweed/600/600',
     '["https://picsum.photos/seed/seaweed-1/600/600","https://picsum.photos/seed/seaweed-2/600/600"]',
     7, 'ON_SHELF'),

(7,  '闽南手工鱼丸 500g*2包',
     '厦门鼓浪屿传统工艺，马鲛鱼肉含量85%以上，弹牙鲜甜。',
     'https://picsum.photos/seed/fish-ball/600/600',
     '["https://picsum.photos/seed/fish-ball-1/600/600"]',
     7, 'ON_SHELF'),

(8,  '小米米家扫拖机器人 3C',
     'LDS激光导航，5000Pa吸力，扫拖一体，支持米家APP远程控制。',
     'https://picsum.photos/seed/robot-vacuum/600/600',
     '["https://picsum.photos/seed/robot-vacuum-1/600/600","https://picsum.photos/seed/robot-vacuum-2/600/600"]',
     8, 'ON_SHELF'),

(9,  '重庆老火锅底料 500g 特辣/微辣可选',
     '正宗重庆牛油火锅底料，选用四川汉源花椒、郫县豆瓣，麻辣鲜香，一料多用。',
     'https://picsum.photos/seed/hotpot-base/600/600',
     '["https://picsum.photos/seed/hotpot-base-1/600/600","https://picsum.photos/seed/hotpot-base-2/600/600"]',
     5, 'ON_SHELF'),

(10, '北欧风旋转收纳盘 双层',
    '厨房调料架、化妆品旋转置物架，360°旋转防滑，承重稳固。',
    'https://picsum.photos/seed/storage-rack/600/600',
    '["https://picsum.photos/seed/storage-rack-1/600/600","https://picsum.photos/seed/storage-rack-2/600/600"]',
    9, 'ON_SHELF'),

(3,  'SK-II 神仙水 230ml',
     'PITERA精华，改善肤质，细致毛孔，让肌肤晶莹剔透。',
     'https://picsum.photos/seed/skii/600/600',
     '["https://picsum.photos/seed/skii-1/600/600"]',
     3, 'OFF_SHELF'),

(2,  '冬季加厚羽绒服 长款过膝',
     '90%白鹅绒填充，防风防水面料，连帽设计，零下30度御寒。',
     'https://picsum.photos/seed/down-jacket/600/600',
     '["https://picsum.photos/seed/down-jacket-1/600/600"]',
     2, 'OFF_SHELF'),

(1,  'Apple AirPods Pro 2 (USB-C)',
     'H2芯片，自适应降噪，通透模式，个性化空间音频，6小时续航。',
     'https://picsum.photos/seed/airpods/600/600',
     '["https://picsum.photos/seed/airpods-1/600/600","https://picsum.photos/seed/airpods-2/600/600"]',
     1, 'OFF_SHELF'),

(8,  '已下架-智能灯泡套装',
     'WiFi智能灯泡，16万色可选，语音控制，定时开关。',
     'https://picsum.photos/seed/smart-bulb/600/600',
     '["https://picsum.photos/seed/smart-bulb-1/600/600"]',
     8, 'OFF_SHELF'),

(2,  '已删除-仿皮草外套',
     '已删除商品，不再展示。',
     'https://picsum.photos/seed/deleted-coat/600/600',
     '["https://picsum.photos/seed/deleted-coat-1/600/600"]',
     2, 'DELETED'),

(10, '已删除-过期收纳盒',
     '已删除商品，不再展示。',
     'https://picsum.photos/seed/deleted-box/600/600',
     '["https://picsum.photos/seed/deleted-box-1/600/600"]',
     9, 'DELETED');

-- ============================================================
-- 测试数据: SKU (每个商品 1-3 个规格)
-- ============================================================

INSERT INTO t_product_sku (product_id, sku_code, sku_name, specifications, price, image) VALUES
-- 1. iPhone手机壳
(1, 'IP16PM-BLK', 'iPhone 16 Pro Max 黑色',     '{"颜色":"黑色","型号":"16 Pro Max"}', 49.00,  'https://picsum.photos/seed/iphone-case-black/400/400'),
(1, 'IP16PM-BLU', 'iPhone 16 Pro Max 深蓝色',   '{"颜色":"深蓝色","型号":"16 Pro Max"}', 49.00,  'https://picsum.photos/seed/iphone-case-blue/400/400'),
(1, 'IP16PM-RED', 'iPhone 16 Pro Max 红色',     '{"颜色":"红色","型号":"16 Pro Max"}', 49.00,  'https://picsum.photos/seed/iphone-case-red/400/400'),

-- 2. 氮化镓充电器
(2, 'ANK65W-WHT', 'Anker 65W 白色', '{"颜色":"白色"}', 159.00, 'https://picsum.photos/seed/gan-white/400/400'),
(2, 'ANK65W-BLK', 'Anker 65W 黑色', '{"颜色":"黑色"}', 159.00, 'https://picsum.photos/seed/gan-black/400/400'),

-- 3. 碎花连衣裙
(3, 'DRS-S-SML', '碎花连衣裙 S码',  '{"尺码":"S","颜色":"碎花蓝"}', 199.00, 'https://picsum.photos/seed/dress-s/400/400'),
(3, 'DRS-M-SML', '碎花连衣裙 M码',  '{"尺码":"M","颜色":"碎花蓝"}', 199.00, 'https://picsum.photos/seed/dress-m/400/400'),
(3, 'DRS-L-SML', '碎花连衣裙 L码',  '{"尺码":"L","颜色":"碎花蓝"}', 199.00, 'https://picsum.photos/seed/dress-l/400/400'),

-- 4. 纯棉衬衫
(4, 'SHT-S-WHT', '纯棉衬衫 S码 白色',  '{"尺码":"S","颜色":"白色"}', 129.00, 'https://picsum.photos/seed/shirt-s/400/400'),
(4, 'SHT-M-WHT', '纯棉衬衫 M码 白色',  '{"尺码":"M","颜色":"白色"}', 129.00, 'https://picsum.photos/seed/shirt-m/400/400'),

-- 5. 兰蔻精华液 (只有一个规格)
(5, 'LNC-SRM-50', '小黑瓶精华液 50ml', '{"规格":"50ml"}', 1080.00, 'https://picsum.photos/seed/serum-sku/400/400'),

-- 6. MAC口红
(6, 'MAC-CHILI',   'Chili 小辣椒',      '{"色号":"Chili"}',   170.00, 'https://picsum.photos/seed/lipstick-chili/400/400'),
(6, 'MAC-RUBYWOO', 'Ruby Woo 复古红',   '{"色号":"Ruby Woo"}', 170.00, 'https://picsum.photos/seed/lipstick-ruby/400/400'),

-- 7. 三体全集 (只有一个规格)
(7, '3BODY-SET', '三体全集典藏版', '{"册数":"全3册"}', 99.00, 'https://picsum.photos/seed/threebody-sku/400/400'),

-- 8. 原则
(8, 'PRINCIPLES', '原则 精装版', '{"版本":"精装"}', 68.00, 'https://picsum.photos/seed/principles-sku/400/400'),

-- 9. 牛肉丸
(9, 'BB-BEEF',     '手打牛肉丸 纯牛肉',   '{"口味":"原味"}', 89.00,  'https://picsum.photos/seed/beef-original/400/400'),
(9, 'BB-BEEF-SPC', '手打牛肉丸 黑椒味',   '{"口味":"黑椒味"}', 89.00,  'https://picsum.photos/seed/beef-pepper/400/400'),

-- 10. 砂锅粥料包
(10, 'CONGEE-CRB', '虾蟹粥料包', '{"口味":"虾蟹组合"}', 59.00, 'https://picsum.photos/seed/congee-crab/400/400'),

-- 11. 碧螺春
(11, 'BLC-250',  '碧螺春 250g 礼盒装', '{"规格":"250g"}', 388.00, 'https://picsum.photos/seed/biluochun-sku/400/400'),
(11, 'BLC-500',  '碧螺春 500g 礼盒装', '{"规格":"500g"}', 728.00, 'https://picsum.photos/seed/biluochun-sku2/400/400'),

-- 12. 紫菜
(12, 'SW-3PK', '头水紫菜 3包装', '{"规格":"100g*3包"}', 39.00, 'https://picsum.photos/seed/seaweed-sku/400/400'),

-- 13. 鱼丸
(13, 'FB-2PK', '手工鱼丸 2包装', '{"规格":"500g*2包"}', 69.00, 'https://picsum.photos/seed/fishball-sku/400/400'),

-- 14. 扫拖机器人
(14, 'MI-ROBOT3C', '米家扫拖机器人3C', '{"颜色":"白色"}', 1299.00, 'https://picsum.photos/seed/robot3c-sku/400/400'),

-- 15. 火锅底料
(15, 'HP-SPCY', '特辣火锅底料 500g', '{"辣度":"特辣"}', 39.00, 'https://picsum.photos/seed/hotpot-spicy/400/400'),
(15, 'HP-MILD', '微辣火锅底料 500g', '{"辣度":"微辣"}', 39.00, 'https://picsum.photos/seed/hotpot-mild/400/400'),

-- 16. 旋转收纳盘
(16, 'SR-WHT-2', '旋转收纳盘 白色双层', '{"颜色":"白色","层数":"双层"}', 69.00, 'https://picsum.photos/seed/storage-white/400/400'),
(16, 'SR-GRY-2', '旋转收纳盘 灰色双层', '{"颜色":"灰色","层数":"双层"}', 69.00, 'https://picsum.photos/seed/storage-gray/400/400'),

-- 17. SK-II 神仙水 (下架)
(17, 'SKII-230', '神仙水 230ml', '{"规格":"230ml"}', 1540.00, 'https://picsum.photos/seed/skii-sku/400/400'),

-- 18. 羽绒服 (下架)
(18, 'DJ-M-BLK', '羽绒服 M码 黑色', '{"尺码":"M","颜色":"黑色"}', 899.00, 'https://picsum.photos/seed/jacket-black/400/400'),
(18, 'DJ-L-BLK', '羽绒服 L码 黑色', '{"尺码":"L","颜色":"黑色"}', 899.00, 'https://picsum.photos/seed/jacket-black-l/400/400'),

-- 19. AirPods Pro (下架)
(19, 'APP2-USBC', 'AirPods Pro 2 USB-C', '{"版本":"USB-C"}', 1799.00, 'https://picsum.photos/seed/airpods-sku/400/400'),

-- 20. 智能灯泡 (下架)
(20, 'BULB-RGB', '智能灯泡 RGB版', '{"颜色":"RGB"}', 79.00, 'https://picsum.photos/seed/bulb-sku/400/400'),

-- 21. 仿皮草外套 (已删除)
(21, 'FC-M-BRN', '仿皮草外套 M码 棕色', '{"尺码":"M","颜色":"棕色"}', 399.00, 'https://picsum.photos/seed/coat-brown/400/400'),

-- 22. 过期收纳盒 (已删除)
(22, 'BOX-SML', '收纳盒套装 小号', '{"尺寸":"小号"}', 29.00, 'https://picsum.photos/seed/box-small/400/400');

-- ============================================================
-- 测试数据: 库存 (每个 SKU 对应一条库存记录)
-- ============================================================

INSERT INTO t_inventory (sku_id, stock, locked_stock) VALUES
(1,  500, 0),   -- iPhone壳 黑色
(2,  320, 5),   -- iPhone壳 深蓝
(3,  150, 0),   -- iPhone壳 红色
(4,  800, 10),  -- 充电器 白
(5,  600, 0),   -- 充电器 黑
(6,  100, 3),   -- 连衣裙 S
(7,  200, 0),   -- 连衣裙 M
(8,  150, 0),   -- 连衣裙 L
(9,  300, 2),   -- 衬衫 S
(10, 250, 0),   -- 衬衫 M
(11, 80,  1),   -- 精华液
(12, 400, 0),   -- 口红 Chili
(13, 350, 5),   -- 口红 RubyWoo
(14, 600, 0),   -- 三体全集
(15, 200, 0),   -- 原则
(16, 100, 8),   -- 牛肉丸 原味
(17, 80,  0),   -- 牛肉丸 黑椒
(18, 150, 0),   -- 砂锅粥料包
(19, 50,  2),   -- 碧螺春 250g
(20, 30,  0),   -- 碧螺春 500g
(21, 500, 0),   -- 紫菜
(22, 200, 0),   -- 鱼丸
(23, 120, 3),   -- 机器人
(24, 300, 0),   -- 火锅底料 特辣
(25, 250, 0),   -- 火锅底料 微辣
(26, 400, 0),   -- 收纳盘 白
(27, 350, 0),   -- 收纳盘 灰
(28, 30,  0),   -- SKII 神仙水 (下架)
(29, 70,  0),   -- 羽绒服 M
(30, 45,  0),   -- 羽绒服 L
(31, 0,   0),   -- AirPods (下架，库存0)
(32, 300, 0),   -- 智能灯泡 (下架)
(33, 0,   0),   -- 仿皮草外套 (已删除)
(34, 500, 0);   -- 收纳盒 (已删除)
