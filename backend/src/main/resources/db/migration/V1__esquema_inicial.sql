-- Esquema inicial

CREATE TABLE sigma_users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NULL ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at    DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sigma_assets (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(120) NOT NULL,
    area            VARCHAR(60)  NOT NULL,
    location        VARCHAR(120) NULL,
    criticality     VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    manufacturer    VARCHAR(80)  NULL,
    model           VARCHAR(80)  NULL,
    commissioned_at DATE         NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NULL ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_assets_code UNIQUE (code),
    INDEX idx_assets_area (area),
    INDEX idx_assets_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sigma_spare_parts (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    sku           VARCHAR(30)   NOT NULL,
    name          VARCHAR(120)  NOT NULL,
    unit          VARCHAR(16)   NOT NULL,
    stock         INT           NOT NULL DEFAULT 0,
    reorder_point INT           NOT NULL DEFAULT 0,
    unit_cost     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)   NULL ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at    DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_spare_parts_sku UNIQUE (sku),
    CONSTRAINT chk_spare_parts_stock CHECK (stock >= 0),
    CONSTRAINT chk_spare_parts_reorder_point CHECK (reorder_point >= 0),
    INDEX idx_spare_parts_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sigma_preventive_plans (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    name             VARCHAR(120)  NOT NULL,
    asset_id         BIGINT        NOT NULL,
    frequency_days   INT           NOT NULL,
    next_due_date    DATE          NOT NULL,
    task_description VARCHAR(1000) NOT NULL,
    priority         VARCHAR(32)   NOT NULL,
    status           VARCHAR(32)   NOT NULL,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NULL ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at       DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_preventive_plans_assets FOREIGN KEY (asset_id) REFERENCES sigma_assets (id),
    CONSTRAINT chk_preventive_plans_frequency CHECK (frequency_days BETWEEN 1 AND 730),
    INDEX idx_preventive_plans_asset_id (asset_id),
    -- Consulta del generador automatico: planes activos con vencimiento <= hoy
    INDEX idx_preventive_plans_status_next_due_date (status, next_due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sigma_work_orders (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    code               VARCHAR(20)   NULL,
    asset_id           BIGINT        NOT NULL,
    type               VARCHAR(32)   NOT NULL,
    priority           VARCHAR(32)   NOT NULL,
    status             VARCHAR(32)   NOT NULL,
    title              VARCHAR(150)  NOT NULL,
    description        VARCHAR(1000) NULL,
    assigned_to_id     BIGINT        NULL,
    created_by_id      BIGINT        NOT NULL,
    preventive_plan_id BIGINT        NULL,
    due_date           DATE          NOT NULL,
    failure_at         DATETIME(6)   NULL,
    started_at         DATETIME(6)   NULL,
    closed_at          DATETIME(6)   NULL,
    labor_hours        DECIMAL(6,2)  NULL,
    resolution_notes   VARCHAR(1000) NULL,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_work_orders_code UNIQUE (code),
    CONSTRAINT fk_work_orders_assets FOREIGN KEY (asset_id) REFERENCES sigma_assets (id),
    CONSTRAINT fk_work_orders_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES sigma_users (id),
    CONSTRAINT fk_work_orders_created_by FOREIGN KEY (created_by_id) REFERENCES sigma_users (id),
    CONSTRAINT fk_work_orders_preventive_plans FOREIGN KEY (preventive_plan_id) REFERENCES sigma_preventive_plans (id),
    INDEX idx_work_orders_asset_id (asset_id),
    INDEX idx_work_orders_assigned_to_id (assigned_to_id),
    INDEX idx_work_orders_created_by_id (created_by_id),
    INDEX idx_work_orders_preventive_plan_id (preventive_plan_id),
    -- Listados filtrados por estatus y ordenados por fecha compromiso
    INDEX idx_work_orders_status_due_date (status, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sigma_work_order_parts (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT        NOT NULL,
    spare_part_id BIGINT        NOT NULL,
    quantity      INT           NOT NULL,
    unit_cost     DECIMAL(12,2) NOT NULL,
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)   NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_work_order_parts_work_orders FOREIGN KEY (work_order_id) REFERENCES sigma_work_orders (id),
    CONSTRAINT fk_work_order_parts_spare_parts FOREIGN KEY (spare_part_id) REFERENCES sigma_spare_parts (id),
    CONSTRAINT chk_work_order_parts_quantity CHECK (quantity > 0),
    INDEX idx_work_order_parts_work_order_id (work_order_id),
    INDEX idx_work_order_parts_spare_part_id (spare_part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
