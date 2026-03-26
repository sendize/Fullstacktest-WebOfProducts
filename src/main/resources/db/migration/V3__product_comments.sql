CREATE TABLE product_comments
(
    internal_id         UUID PRIMARY KEY NOT NULL,
    product_internal_id UUID             NOT NULL,
    variant_internal_id UUID NULL,

    reviewer_name       VARCHAR(120)     NOT NULL,
    reviewer_email      VARCHAR(255)     NOT NULL,
    comment_text        TEXT             NOT NULL,
    rating              INTEGER          NOT NULL,

    created_at          TIMESTAMP        NOT NULL DEFAULT NOW(),
    modified_at         TIMESTAMP        NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_product_comments_product
        FOREIGN KEY (product_internal_id) REFERENCES products (internal_id) ON DELETE CASCADE,
    CONSTRAINT fk_product_comments_variant
        FOREIGN KEY (variant_internal_id) REFERENCES product_variants (internal_id) ON DELETE SET NULL,
    CONSTRAINT chk_product_comments_rating
        CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_product_comments_product_rating_created_at
    ON product_comments (product_internal_id, rating DESC, created_at DESC);

CREATE INDEX idx_product_comments_variant_created_at
    ON product_comments (variant_internal_id, created_at DESC);
