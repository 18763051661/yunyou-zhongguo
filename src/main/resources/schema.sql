-- ======================================================================
-- 全国景点智能导览推荐 · 在线虚拟游览 · AI 助教讲解系统  MySQL 8 建表脚本
-- 幂等设计: CREATE TABLE IF NOT EXISTS, 应用启动时仅在空库情况下执行 data.sql
-- ======================================================================

-- 景点分类
CREATE TABLE IF NOT EXISTS category (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(50)  NOT NULL,
    icon      VARCHAR(20),
    sort_no   INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 景点(全国)
CREATE TABLE IF NOT EXISTS scenic_spot (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(80)  NOT NULL,
    category_id      BIGINT       NOT NULL,
    province         VARCHAR(20)  NOT NULL COMMENT '省份/直辖市/自治区',
    city             VARCHAR(30)  COMMENT '所在城市',
    region           VARCHAR(10)  COMMENT '七大地理分区: 华北/东北/华东/华中/华南/西南/西北',
    alias            VARCHAR(160),
    summary          VARCHAR(255),
    description      TEXT,
    highlights       VARCHAR(600),
    tags             VARCHAR(300),
    image_prompt     VARCHAR(400),
    rating           DOUBLE DEFAULT 4.8,
    rating_count     INT DEFAULT 0,
    popularity       INT DEFAULT 0,
    price            DECIMAL(10,2) DEFAULT 0,
    open_time        VARCHAR(40),
    duration_minutes INT DEFAULT 90,
    difficulty       INT DEFAULT 1,
    lng              DOUBLE COMMENT '经度',
    lat              DOUBLE COMMENT '纬度',
    address          VARCHAR(160),
    suitable_for     VARCHAR(160),
    tips             VARCHAR(600),
    status           INT DEFAULT 1,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_spot_province (province),
    INDEX idx_spot_region (region),
    INDEX idx_spot_category (category_id),
    INDEX idx_spot_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 虚拟游览路线(全国主题)
CREATE TABLE IF NOT EXISTS route (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(80) NOT NULL,
    subtitle         VARCHAR(200),
    description      TEXT,
    duration_minutes INT DEFAULT 180,
    difficulty       INT DEFAULT 1,
    tags             VARCHAR(300),
    suitable_for     VARCHAR(160),
    cover_prompt     VARCHAR(400),
    sort_no          INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 路线-景点 关联(含分步讲解词)
CREATE TABLE IF NOT EXISTS route_spot (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id     BIGINT NOT NULL,
    spot_id      BIGINT NOT NULL,
    day_no       INT DEFAULT 1,
    step_no      INT DEFAULT 1,
    stay_minutes INT DEFAULT 40,
    narration    TEXT,
    INDEX idx_route_spot_route (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password        VARCHAR(100) NOT NULL,
    nickname        VARCHAR(50),
    avatar          VARCHAR(255),
    preference_tags VARCHAR(255),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 收藏
CREATE TABLE IF NOT EXISTS favorite (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    spot_id    BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_favorite (user_id, spot_id),
    INDEX idx_fav_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 评论评分
CREATE TABLE IF NOT EXISTS spot_comment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    spot_id    BIGINT NOT NULL,
    rating     INT NOT NULL,
    content    VARCHAR(600),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_comment_spot (spot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 在线虚拟游览打卡记录
CREATE TABLE IF NOT EXISTS tour_record (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_id    BIGINT,
    route_id   BIGINT,
    spot_id    BIGINT NOT NULL,
    visited_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tour (session_id, spot_id),
    INDEX idx_tour_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 景区门票订单 (沙箱支付)
CREATE TABLE IF NOT EXISTS ticket_order (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(32) NOT NULL UNIQUE COMMENT '内部订单号 SN2026...',
    user_id         BIGINT,
    spot_id         BIGINT NOT NULL,
    spot_name       VARCHAR(80),
    quantity        INT NOT NULL DEFAULT 1,
    unit_price      DECIMAL(10,2) NOT NULL,
    total_amount    DECIMAL(10,2) NOT NULL,
    contact_name    VARCHAR(50),
    contact_phone   VARCHAR(20),
    pay_status      VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/CANCELED',
    pay_method      VARCHAR(16) COMMENT 'WECHAT/ALIPAY/SANDBOX',
    pay_time        DATETIME,
    visit_date      VARCHAR(16) COMMENT 'yyyy-MM-dd 计划游玩日期',
    remark          VARCHAR(255),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_user (user_id),
    INDEX idx_order_spot (spot_id),
    INDEX idx_order_status (pay_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 省份/直辖市/自治区 维度表 (七大地理分区)
-- 说明: scenic_spot.province 保留 VARCHAR 冗余列做查询加速(反范式读写权衡),
--      同时维护 province 维度表以便独立管理省份元数据、构建地理分布统计。
CREATE TABLE IF NOT EXISTS province (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(20) NOT NULL UNIQUE COMMENT '省份/直辖市/自治区 中文名称',
    region  VARCHAR(10) NOT NULL COMMENT '七大地理分区: 华北/东北/华东/华中/华南/西南/西北',
    sort_no INT DEFAULT 0,
    INDEX idx_prov_region (region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 省份种子数据: INSERT IGNORE + UNIQUE(name) 保证幂等, 已存在行自动跳过
INSERT IGNORE INTO province (id, name, region, sort_no) VALUES
  (1,'吉林','东北',1), (2,'辽宁','东北',2), (3,'黑龙江','东北',3),
  (4,'上海','华东',1), (5,'安徽','华东',2), (6,'山东','华东',3),
  (7,'江苏','华东',4), (8,'江西','华东',5), (9,'浙江','华东',6), (10,'福建','华东',7),
  (11,'河南','华中',1), (12,'湖北','华中',2), (13,'湖南','华中',3),
  (14,'内蒙古','华北',1), (15,'北京','华北',2), (16,'天津','华北',3),
  (17,'山西','华北',4), (18,'河北','华北',5),
  (19,'广东','华南',1), (20,'广西','华南',2), (21,'海南','华南',3),
  (22,'宁夏','西北',1), (23,'新疆','西北',2), (24,'甘肃','西北',3),
  (25,'陕西','西北',4), (26,'青海','西北',5),
  (27,'云南','西南',1), (28,'四川','西南',2), (29,'西藏','西南',3),
  (30,'贵州','西南',4), (31,'重庆','西南',5);

-- 管理端操作日志(AOP 切面自动写入, 幂等建表)
CREATE TABLE IF NOT EXISTS operation_log (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  operator   VARCHAR(50)  NOT NULL COMMENT '操作人(管理员账号)',
  action     VARCHAR(50)  NOT NULL COMMENT '动作(新增景点/修改景点/删除景点)',
  target     VARCHAR(200) NULL     COMMENT '操作对象(景点名称 + ID)',
  detail     VARCHAR(500) NULL     COMMENT '补充信息(触发方法)',
  ip         VARCHAR(45)  NULL     COMMENT '操作来源IP',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_oplog_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理端操作日志(AOP 审计)';

-- 用户出游偏好问卷(智能规划表单提交)
CREATE TABLE IF NOT EXISTS travel_preference (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  nickname   VARCHAR(50)  NULL COMMENT '用户昵称',
  days       INT          NOT NULL COMMENT '计划天数',
  provinces  VARCHAR(200) NULL COMMENT '目标省份(逗号分隔)',
  categories VARCHAR(200) NULL COMMENT '偏好类型(逗号分隔: 山水/古迹/古镇/主题乐园/民俗)',
  budget     VARCHAR(30)  NULL COMMENT '预算区间',
  companions VARCHAR(30)  NULL COMMENT '同行: 独自/情侣/家庭/朋友群',
  transport  VARCHAR(30)  NULL COMMENT '交通: 自驾/高铁/飞机/无所谓',
  focus      VARCHAR(500) NULL COMMENT '特殊关注点(自由文本)',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tp_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能规划-用户偏好问卷';

-- AI 生成的个性化出游路线
CREATE TABLE IF NOT EXISTS travel_plan (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  pref_id      BIGINT       NULL COMMENT '关联偏好问卷 ID',
  title        VARCHAR(100) NOT NULL COMMENT '推荐路线标题',
  summary      VARCHAR(500) NULL COMMENT '一句话推荐理由',
  days         INT          NOT NULL,
  spots_json   TEXT         NULL COMMENT '推荐景点列表 JSON(含 day/stock/id/name/province/price)',
  itinerary    TEXT         NULL COMMENT 'AI 生成的逐日行程说明(Markdown)',
  provider     VARCHAR(30)  NULL COMMENT '命中的 AI 引擎(deepseek/qwen/doubao/local)',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_tp_pref (pref_id),
  KEY idx_tp_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能规划-AI 生成路线推荐';

-- VR 环景舱游览打卡(每次进入 VR 全景记录一条, 支撑 VR 热度统计)
CREATE TABLE IF NOT EXISTS vr_view_log (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  spot_id    BIGINT       NOT NULL COMMENT '景点 ID',
  session_id VARCHAR(64)  NULL COMMENT '浏览器会话标识',
  viewed_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_vr_spot (spot_id),
  KEY idx_vr_time (viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='VR 环景舱游览记录';
