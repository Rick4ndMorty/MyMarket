-- ============================================================
-- db_user: 用户服务数据库
-- ============================================================

CREATE DATABASE IF NOT EXISTS db_user
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE db_user;

-- 用户表
CREATE TABLE t_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username        VARCHAR(64)     NOT NULL COMMENT '用户名',
    password        VARCHAR(256)    NOT NULL COMMENT '密码(BCrypt)',
    email           VARCHAR(128)    DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(32)     DEFAULT NULL COMMENT '手机号',
    avatar          VARCHAR(512)    DEFAULT NULL COMMENT '头像URL',
    role            VARCHAR(16)     NOT NULL DEFAULT 'BUYER' COMMENT '角色: BUYER/SELLER/ADMIN',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_phone (phone),
    KEY idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 收货地址表
CREATE TABLE t_user_address (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    receiver_name   VARCHAR(64)     NOT NULL COMMENT '收件人姓名',
    receiver_phone  VARCHAR(32)     NOT NULL COMMENT '收件人电话',
    province        VARCHAR(64)     NOT NULL COMMENT '省份',
    city            VARCHAR(64)     NOT NULL COMMENT '城市',
    district        VARCHAR(64)     NOT NULL COMMENT '区县',
    detail          VARCHAR(256)    NOT NULL COMMENT '详细地址',
    is_default      TINYINT         NOT NULL DEFAULT 0 COMMENT '是否默认: 1是 0否',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

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
-- 测试数据: 20 个用户 (密码均为 123456, BCrypt 加密)
-- 角色分布: 10 BUYER, 7 SELLER, 2 BUYER+SELLER, 1 ADMIN
-- ============================================================

INSERT INTO t_user (username, password, email, phone, avatar, role, status) VALUES
('buyer01',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'buyer01@test.com',   '13800010001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=buyer01',   'BUYER',  1),
('buyer02',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'buyer02@test.com',   '13800010002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=buyer02',   'BUYER',  1),
('buyer03',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'buyer03@test.com',   '13800010003', 'https://api.dicebear.com/7.x/avataaars/svg?seed=buyer03',   'BUYER',  1),
('buyer04',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'buyer04@test.com',   '13800010004', 'https://api.dicebear.com/7.x/avataaars/svg?seed=buyer04',   'BUYER',  1),
('buyer05',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'buyer05@test.com',   '13800010005', 'https://api.dicebear.com/7.x/avataaars/svg?seed=buyer05',   'BUYER',  1),
('buyer06',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'buyer06@test.com',   '13800010006', 'https://api.dicebear.com/7.x/avataaars/svg?seed=buyer06',   'BUYER',  1),
('buyer07',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'buyer07@test.com',   '13800010007', 'https://api.dicebear.com/7.x/avataaars/svg?seed=buyer07',   'BUYER',  1),
('buyer08',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'buyer08@test.com',   '13800010008', 'https://api.dicebear.com/7.x/avataaars/svg?seed=buyer08',   'BUYER',  1),
('buyer09',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'buyer09@test.com',   '13800010009', 'https://api.dicebear.com/7.x/avataaars/svg?seed=buyer09',   'BUYER',  1),
('buyer10',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'buyer10@test.com',   '13800010010', 'https://api.dicebear.com/7.x/avataaars/svg?seed=buyer10',   'BUYER',  0),  -- 禁用
('seller01',     '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'seller01@test.com',  '13800020001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=seller01',  'SELLER', 1),
('seller02',     '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'seller02@test.com',  '13800020002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=seller02',  'SELLER', 1),
('seller03',     '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'seller03@test.com',  '13800020003', 'https://api.dicebear.com/7.x/avataaars/svg?seed=seller03',  'SELLER', 1),
('seller04',     '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'seller04@test.com',  '13800020004', 'https://api.dicebear.com/7.x/avataaars/svg?seed=seller04',  'SELLER', 1),
('seller05',     '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'seller05@test.com',  '13800020005', 'https://api.dicebear.com/7.x/avataaars/svg?seed=seller05',  'SELLER', 1),
('seller06',     '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'seller06@test.com',  '13800020006', 'https://api.dicebear.com/7.x/avataaars/svg?seed=seller06',  'SELLER', 1),
('seller07',     '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'seller07@test.com',  '13800020007', 'https://api.dicebear.com/7.x/avataaars/svg?seed=seller07',  'SELLER', 1),
('duo_user01',   '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'duo01@test.com',     '13800030001', 'https://api.dicebear.com/7.x/avataaars/svg?seed=duo01',     'SELLER', 1),  -- 同时是买家也是卖家
('duo_user02',   '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'duo02@test.com',     '13800030002', 'https://api.dicebear.com/7.x/avataaars/svg?seed=duo02',     'SELLER', 1),
('admin01',      '$2b$10$GuA66GFqdJCku3r9ukNeieTFsHNUq2bzjv5mMRVHwG7BR4GNesQ3y', 'admin@tradestation.com', '13800099999', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin',   'ADMIN',  1);

-- ============================================================
-- 测试数据: 20 个收货地址
-- ============================================================

INSERT INTO t_user_address (user_id, receiver_name, receiver_phone, province, city, district, detail, is_default) VALUES
(1,  '张三',  '13800010001', '广东省', '深圳市', '南山区', '科技园路1号创新大厦A座1201', 1),
(1,  '张三',  '13800010001', '广东省', '深圳市', '南山区', '白石路202号腾讯滨海大厦', 0),
(2,  '李四',  '13800010002', '北京市', '北京市', '朝阳区', '望京街道阜通东大街6号院3号楼', 1),
(3,  '王五',  '13800010003', '上海市', '上海市', '浦东新区', '张江高科技园区祖冲之路1500号', 1),
(4,  '赵六',  '13800010004', '浙江省', '杭州市', '西湖区', '文三路478号华星时代广场B座', 1),
(5,  '孙七',  '13800010005', '广东省', '广州市', '天河区', '天河路385号太古汇一座1802', 1),
(6,  '周八',  '13800010006', '四川省', '成都市', '高新区', '天府大道中段688号紫光大楼', 1),
(7,  '吴九',  '13800010007', '湖北省', '武汉市', '洪山区', '珞喻路1037号华中科技大学', 1),
(8,  '郑十',  '13800010008', '江苏省', '南京市', '玄武区', '中山东路301号南京图书馆旁小区2栋', 1),
(9,  '钱十一','13800010009', '陕西省', '西安市', '雁塔区', '锦业路1号都市之门C座508', 1),
(10, '陈十二','13800010010', '湖南省', '长沙市', '岳麓区', '麓谷大道658号麓谷信息港A栋', 1),
(11, '刘店主','13800020001', '广东省', '深圳市', '宝安区', '西乡大道300号华丰互联网创意园', 1),
(12, '黄店主','13800020002', '浙江省', '杭州市', '余杭区', '文一西路1500号阿里巴巴西溪园区', 1),
(13, '林店主','13800020003', '上海市', '上海市', '徐汇区', '虹漕路88号越界创意园20号楼', 1),
(14, '何店主','13800020004', '北京市', '北京市', '海淀区', '中关村大街1号鼎好电子大厦A座', 1),
(15, '邓店主','13800020005', '广东省', '广州市', '番禺区', '迎宾路99号敏捷广场B栋901', 1),
(16, '许店主','13800020006', '江苏省', '苏州市', '工业园区', '星湖街328号创意产业园5号楼', 1),
(17, '冯店主','13800020007', '福建省', '厦门市', '思明区', '软件园二期望海路39号楼', 1),
(18, '双重用户A','13800030001', '广东省', '深圳市', '龙岗区', '坂田街道雪岗路2018号天安云谷', 1),
(19, '双重用户B','13800030002', '重庆市', '重庆市', '渝北区', '光电园麒麟座D栋12楼', 1),
(20, '系统管理员','13800099999', '广东省', '深圳市', '南山区', '后海大道2378号深圳湾科技生态园', 1);
