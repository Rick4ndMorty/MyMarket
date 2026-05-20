-- ============================================================
-- db_seata: Seata Server 持久化数据库
-- ============================================================

CREATE DATABASE IF NOT EXISTS db_seata
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE db_seata;

-- 全局事务表
CREATE TABLE IF NOT EXISTS global_table (
    xid                       VARCHAR(128)  NOT NULL,
    transaction_id            BIGINT,
    status                    TINYINT       NOT NULL,
    application_id            VARCHAR(32),
    transaction_service_group VARCHAR(32),
    transaction_name          VARCHAR(128),
    timeout                   INT,
    begin_time                BIGINT,
    application_data          VARCHAR(2000),
    gmt_create                DATETIME,
    gmt_modified              DATETIME,
    PRIMARY KEY (xid),
    KEY idx_status_gmt_modified (status, gmt_modified),
    KEY idx_transaction_id (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 分支事务表
CREATE TABLE IF NOT EXISTS branch_table (
    branch_id         BIGINT        NOT NULL,
    xid               VARCHAR(128)  NOT NULL,
    transaction_id    BIGINT,
    resource_group_id VARCHAR(32),
    resource_id       VARCHAR(256),
    branch_type       VARCHAR(8),
    status            TINYINT,
    client_id         VARCHAR(64),
    application_data  VARCHAR(2000),
    gmt_create        DATETIME,
    gmt_modified      DATETIME,
    PRIMARY KEY (branch_id),
    KEY idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 全局锁表
CREATE TABLE IF NOT EXISTS lock_table (
    row_key        VARCHAR(128) NOT NULL,
    xid            VARCHAR(128),
    transaction_id BIGINT,
    branch_id      BIGINT       NOT NULL,
    resource_id    VARCHAR(256),
    table_name     VARCHAR(32),
    pk             VARCHAR(36),
    status         TINYINT      NOT NULL DEFAULT 0,
    gmt_create     DATETIME,
    gmt_modified   DATETIME,
    PRIMARY KEY (row_key),
    KEY idx_branch_id (branch_id),
    KEY idx_xid (xid),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 分布式锁表 (Seata 1.6.1)
CREATE TABLE IF NOT EXISTS distributed_lock (
    lock_key   VARCHAR(20)  NOT NULL,
    lock_value VARCHAR(20)  NOT NULL,
    expire     BIGINT,
    PRIMARY KEY (lock_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
