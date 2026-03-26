CREATE TABLE catalog_activity_events
(
    internal_id         UUID PRIMARY KEY NOT NULL,
    product_internal_id UUID NULL,
    variant_internal_id UUID NULL,

    event_type          VARCHAR(100)     NOT NULL,
    severity            VARCHAR(20)      NOT NULL,
    source_label        VARCHAR(120)     NOT NULL,
    headline            VARCHAR(255)     NOT NULL,
    detail_text         TEXT             NOT NULL,
    metadata_json       JSONB            NOT NULL DEFAULT '{}'::jsonb,

    created_at          TIMESTAMP        NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_catalog_activity_product
        FOREIGN KEY (product_internal_id) REFERENCES products (internal_id) ON DELETE SET NULL,
    CONSTRAINT fk_catalog_activity_variant
        FOREIGN KEY (variant_internal_id) REFERENCES product_variants (internal_id) ON DELETE SET NULL,
    CONSTRAINT chk_catalog_activity_severity
        CHECK (severity IN ('info', 'success', 'warning', 'critical'))
);

CREATE INDEX idx_catalog_activity_events_created_at
    ON catalog_activity_events (created_at DESC);

CREATE INDEX idx_catalog_activity_events_product_created_at
    ON catalog_activity_events (product_internal_id, created_at DESC);

CREATE INDEX idx_catalog_activity_events_variant_created_at
    ON catalog_activity_events (variant_internal_id, created_at DESC);

CREATE INDEX idx_catalog_activity_events_type_created_at
    ON catalog_activity_events (event_type, created_at DESC);
