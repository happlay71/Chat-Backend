-- 创建库
CREATE DATABASE IF NOT EXISTS chat;

-- 切换库
USE chat;

-- 创建群组表
CREATE TABLE group_info
(
    group_id       VARCHAR(12)          NOT NULL COMMENT '群ID',
    group_name     VARCHAR(20)          NULL COMMENT '群组名',
    group_owner_id VARCHAR(12)          NULL COMMENT '群主ID',
    create_time    DATETIME             NULL COMMENT '创建时间',
    group_notice   VARCHAR(500)         NULL COMMENT '群公告',
    join_type      TINYINT(1)           NULL COMMENT '0：直接加入 1：管理员同意后加入',
    status         TINYINT(1) DEFAULT 1 NULL COMMENT '状态 1：正常 0：解散',
    PRIMARY KEY (group_id)
)
COMMENT '群';

-- 创建联系人表
CREATE TABLE user_contact
(
    user_id          VARCHAR(12)                         NOT NULL COMMENT '用户ID',
    contact_id       VARCHAR(12)                         NOT NULL COMMENT '联系人ID或群组ID',
    contact_type     TINYINT(1)                          NULL COMMENT '联系人类型 0:好友 1:群组',
    create_time      DATETIME                            NULL COMMENT '创建时间',
    status           TINYINT(1)                          NULL COMMENT '状态 0:非好友 1:好友 2:已删除好友 3:被好友删除 4:已拉黑好友 5:被好友拉黑',
    last_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (user_id, contact_id)
)
COMMENT '联系人';

-- 创建索引
CREATE INDEX idx_contact_id ON user_contact (contact_id);

-- 创建联系人申请表
CREATE TABLE user_contact_apply
(
    apply_id        INT AUTO_INCREMENT COMMENT '自增ID',
    apply_user_id   VARCHAR(12)  NOT NULL COMMENT '申请人ID',
    receive_user_id VARCHAR(12)  NOT NULL COMMENT '接收人ID',
    contact_type    TINYINT(1)   NOT NULL COMMENT '联系人类型 0:好友 1:群组',
    contact_id      VARCHAR(12)  NULL COMMENT '联系人ID',
    last_apply_time BIGINT       NULL COMMENT '最后申请时间',
    status          TINYINT(1)   NULL COMMENT '状态 0:待处理 1:已同意 2:已拒绝 3:已拉黑',
    apply_info      VARCHAR(100) NULL COMMENT '申请信息',
    PRIMARY KEY (apply_id),
    UNIQUE INDEX idx_key (apply_user_id, receive_user_id, contact_id)
)
COMMENT '联系人申请'
COLLATE = utf8mb4_general_ci;

-- 创建最后申请时间索引
CREATE INDEX idx_last_apply_time ON user_contact_apply (last_apply_time);

-- 创建用户信息表
CREATE TABLE user_info
(
    user_id            VARCHAR(12) NOT NULL COMMENT '用户ID',
    email              VARCHAR(50) NULL COMMENT '邮箱',
    nick_name          VARCHAR(20) NULL COMMENT '昵称',
    join_type          TINYINT(1)  NULL COMMENT '0：直接加入 1：同意后加好友',
    sex                TINYINT(1)  NULL COMMENT '性别 0：女 1：男',
    password           VARCHAR(32) NULL COMMENT '密码',
    personal_signature VARCHAR(50) NULL COMMENT '个性签名',
    status             TINYINT(1)  NULL COMMENT '状态',
    create_time        DATETIME    NULL COMMENT '创建时间',
    last_login_time    DATETIME    NULL COMMENT '最后登录时间',
    area_name          VARCHAR(50) NULL COMMENT '地区',
    area_code          VARCHAR(50) NULL COMMENT '地区编号',
    last_off_time      BIGINT      NULL COMMENT '最后离开时间',
    PRIMARY KEY (user_id),
    UNIQUE INDEX user_info_email_uindex (email)
)
COMMENT '用户信息';

-- 创建靓号表
CREATE TABLE user_info_beauty
(
    id      INT AUTO_INCREMENT COMMENT '自增ID',
    email   VARCHAR(50) NOT NULL COMMENT '邮箱',
    user_id VARCHAR(12) NOT NULL COMMENT '用户ID',
    status  TINYINT(1)  NULL COMMENT '0：未使用 1：已使用',
    PRIMARY KEY (id),
    UNIQUE INDEX user_info_beauty_email_uindex (email),
    UNIQUE INDEX user_info_beauty_user_id_uindex (user_id)
)
COMMENT '靓号';

-- 创建app发布表
CREATE TABLE app_update
(
    id int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    version varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '版本号',
    update_desc varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新描述',
    create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
    status tinyint(1) NULL DEFAULT NULL COMMENT '0:未发布 1:灰度发布 2:全网发布',
    grayscale_uid varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '灰度uid',
    file_type tinyint(1) NULL DEFAULT NULL COMMENT '文件类型: 0:本地文件 1:外链',
    outer_link varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '外链地址',
    PRIMARY KEY (id) USING BTREE,
    UNIQUE KEY unique_version (version) USING BTREE
)
COMMENT='app发布';

CREATE TABLE chat_session
(
    session_id varchar(32) NOT NULL COMMENT '会话ID',
    last_message varchar(500) NULL DEFAULT NULL COMMENT '最后接受的信息',
    last_receive_time bigint(11) NULL DEFAULT NULL COMMENT '最后接受消息时间(毫秒)',
    PRIMARY KEY (session_id) USING BTREE
)
COMMENT='会话信息';

CREATE TABLE chat_message
(
    message_id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息自增ID',
    session_id varchar(32) NOT NULL COMMENT '会话ID',
    message_type tinyint(1) NOT NULL COMMENT '消息类型',
    message_content varchar(500) NULL DEFAULT NULL COMMENT '消息内容',
    send_user_id varchar(12) NULL DEFAULT NULL COMMENT '发送人ID',
    send_user_nick_name varchar(20) NULL DEFAULT NULL COMMENT '发送人昵称',
    send_time bigint(20) NULL DEFAULT NULL COMMENT '发送时间',
    contact_id varchar(12) NOT NULL COMMENT '接收联系人ID',
    contact_type tinyint(1) NULL DEFAULT NULL COMMENT '联系人类型',
    file_size bigint(20) NULL DEFAULT NULL COMMENT '文件大小',
    file_name varchar(200) NULL DEFAULT NULL COMMENT '文件名',
    file_type tinyint(1) NULL DEFAULT NULL COMMENT '文件类型',
    status tinyint(1) NULL DEFAULT 1 COMMENT '状态: 0:正在发送 1:已发送',
    PRIMARY KEY (message_id) USING BTREE,
    INDEX idx_session_id (session_id) USING BTREE,
    INDEX idx_send_user_id (send_user_id) USING BTREE,
    INDEX idx_receive_contact_id (contact_id) USING BTREE,
    INDEX idx_send_time (send_time) USING BTREE
)
COMMENT='聊天消息表';

CREATE TABLE chat_session_user
(
    user_id varchar(12) NOT NULL COMMENT '用户ID',
    contact_id varchar(12) NOT NULL COMMENT '联系人ID',
    session_id varchar(32) NOT NULL COMMENT '会话ID',
    contact_name varchar(20) NULL DEFAULT NULL COMMENT '联系人名称',
    PRIMARY KEY (user_id, contact_id) USING BTREE,
    INDEX idx_user_id (user_id) USING BTREE,
    INDEX idx_session_id (session_id) USING BTREE
)
COMMENT='会话用户';