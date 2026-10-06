-- 蓝水晶商城 v2 骨架初始化脚本（MySQL 8.4）
-- 每个业务库各带一张 Seata AT 模式需要的 undo_log 表。

CREATE DATABASE IF NOT EXISTS `bc-item` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `bc-cart` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `bc-user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `bc-trade` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `bc-pay` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============ Seata undo_log（每个业务库一份） ============
CREATE TABLE IF NOT EXISTS `bc-item`.undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `bc-cart`.undo_log LIKE `bc-item`.undo_log;
CREATE TABLE IF NOT EXISTS `bc-user`.undo_log LIKE `bc-item`.undo_log;
CREATE TABLE IF NOT EXISTS `bc-trade`.undo_log LIKE `bc-item`.undo_log;
CREATE TABLE IF NOT EXISTS `bc-pay`.undo_log LIKE `bc-item`.undo_log;

-- ============ 商品库 ============
USE `bc-item`;
CREATE TABLE IF NOT EXISTS item (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255)  NOT NULL,
    price         INT           NOT NULL COMMENT '价格，单位：分',
    stock         INT           NOT NULL DEFAULT 0,
    image         VARCHAR(1024) DEFAULT NULL,
    category      VARCHAR(64)   DEFAULT NULL,
    brand         VARCHAR(64)   DEFAULT NULL,
    spec          VARCHAR(255)  DEFAULT NULL COMMENT '规格 JSON',
    sold          INT           DEFAULT 0,
    comment_count INT           DEFAULT 0,
    is_ad         TINYINT(1)    DEFAULT 0,
    status        INT           DEFAULT 1 COMMENT '1 上架 2 下架 3 删除',
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO item (id, name, price, stock, image, category, brand, spec, sold, comment_count, is_ad, status)
VALUES
(1, 'Demo Phone 128G', 199900, 100, 'https://via.placeholder.com/200', 'Phone', '蓝水晶商城', '{"color":"black","memory":"128G"}', 0, 0, 0, 1),
(2, 'Demo Headset', 12900, 200, 'https://via.placeholder.com/200', 'Audio', '蓝水晶商城', '{"color":"white"}', 0, 0, 0, 1),
(3, 'Demo Keyboard', 8900, 150, 'https://via.placeholder.com/200', 'Peripheral', '蓝水晶商城', '{"layout":"87"}', 0, 0, 0, 1);

-- ============ 购物车库 ============
USE `bc-cart`;
CREATE TABLE IF NOT EXISTS cart (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    item_id     BIGINT        NOT NULL,
    num         INT           NOT NULL DEFAULT 1,
    name        VARCHAR(255)  DEFAULT NULL,
    spec        VARCHAR(255)  DEFAULT NULL,
    price       INT           DEFAULT NULL COMMENT '加购时价格，单位：分',
    image       VARCHAR(1024) DEFAULT NULL,
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_item (user_id, item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ============ 用户库 ============
USE `bc-user`;
CREATE TABLE IF NOT EXISTS `user` (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL,
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt 摘要',
    phone       VARCHAR(20)  DEFAULT NULL,
    status      INT          NOT NULL DEFAULT 1 COMMENT '0 冻结 1 正常',
    balance     INT          NOT NULL DEFAULT 0 COMMENT '余额，单位：分',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS address (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    province   VARCHAR(32)  DEFAULT NULL,
    city       VARCHAR(32)  DEFAULT NULL,
    town       VARCHAR(32)  DEFAULT NULL,
    mobile     VARCHAR(20)  DEFAULT NULL,
    street     VARCHAR(255) DEFAULT NULL,
    contact    VARCHAR(64)  DEFAULT NULL,
    is_default INT          DEFAULT 0,
    notes      VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 演示账号：demo / 123456（BCrypt），余额 1000000 分 = 10000 元
INSERT INTO `user` (id, username, password, phone, status, balance)
VALUES (1, 'demo', '$2b$10$jMxxCEZ75kfKFFWanwvQr.X2GzhebYYotYaXpD.XT2URDfOmzXa1S', '13800000000', 1, 1000000);

INSERT INTO address (id, user_id, province, city, town, mobile, street, contact, is_default, notes)
VALUES (1, 1, 'Beijing', 'Beijing', 'Haidian', '13800000000', 'Demo Street 1', 'demo', 1, 'demo address');

-- ============ 交易库 ============
USE `bc-trade`;
CREATE TABLE IF NOT EXISTS `order` (
    id           BIGINT   NOT NULL COMMENT '雪花 id',
    total_fee    INT      NOT NULL,
    payment_type INT      DEFAULT NULL,
    user_id      BIGINT   NOT NULL,
    status       INT      NOT NULL DEFAULT 1 COMMENT '1 待支付 2 已支付 3 已关闭',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    pay_time     DATETIME DEFAULT NULL,
    consign_time DATETIME DEFAULT NULL,
    end_time     DATETIME DEFAULT NULL,
    close_time   DATETIME DEFAULT NULL,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS order_detail (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    order_id    BIGINT        NOT NULL,
    item_id     BIGINT        NOT NULL,
    num         INT           NOT NULL,
    name        VARCHAR(255)  DEFAULT NULL,
    spec        VARCHAR(255)  DEFAULT NULL,
    price       INT           DEFAULT NULL,
    image       VARCHAR(1024) DEFAULT NULL,
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS order_logistics (
    order_id          BIGINT       NOT NULL,
    logistics_number  VARCHAR(64)  DEFAULT NULL,
    logistics_company VARCHAR(64)  DEFAULT NULL,
    contact           VARCHAR(64)  DEFAULT NULL,
    mobile            VARCHAR(20)  DEFAULT NULL,
    province          VARCHAR(32)  DEFAULT NULL,
    city              VARCHAR(32)  DEFAULT NULL,
    town              VARCHAR(32)  DEFAULT NULL,
    street            VARCHAR(255) DEFAULT NULL,
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ============ 支付库 ============
USE `bc-pay`;
CREATE TABLE IF NOT EXISTS pay_order (
    id               BIGINT        NOT NULL COMMENT '雪花 id',
    biz_order_no     BIGINT        DEFAULT NULL COMMENT '业务订单号',
    pay_order_no     BIGINT        DEFAULT NULL COMMENT '支付流水号',
    biz_user_id      BIGINT        DEFAULT NULL,
    pay_channel_code VARCHAR(32)   DEFAULT NULL COMMENT 'balance / alipay / wx',
    amount           INT           DEFAULT NULL COMMENT '金额，单位：分',
    pay_type         INT           DEFAULT NULL,
    status           INT           DEFAULT 0 COMMENT '0 待支付 1 已支付 2 已关闭',
    expand_json      VARCHAR(1024) DEFAULT NULL,
    result_code      VARCHAR(64)   DEFAULT NULL,
    result_msg       VARCHAR(255)  DEFAULT NULL,
    pay_success_time DATETIME      DEFAULT NULL,
    pay_over_time    DATETIME      DEFAULT NULL,
    create_time      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_biz_order (biz_order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
