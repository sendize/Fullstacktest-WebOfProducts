CREATE TABLE products
(
    internal_id  UUID PRIMARY KEY NOT NULL,
    external_id  BIGINT           NOT NULL,
    title        VARCHAR(255)     NOT NULL,
    body_html    TEXT             NOT NULL,
    product_type VARCHAR(100)     NOT NULL,
    image_url    TEXT             NOT NULL,

    created_at   TIMESTAMP        NOT NULL DEFAULT NOW(),
    modified_at  TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE product_variants
(
    internal_id         UUID PRIMARY KEY NOT NULL,
    external_id         BIGINT           NOT NULL,
    product_internal_id UUID             NOT NULL,

    sku                 VARCHAR(100)     NOT NULL,
    title               VARCHAR(100)     NOT NULL,
    price               DECIMAL(10, 2)   NOT NULL,
    color               VARCHAR(50)      NOT NULL,
    size                VARCHAR(10)      NOT NULL,
    image_url           TEXT             NOT NULL,

    created_at          TIMESTAMP        NOT NULL DEFAULT NOW(),
    modified_at         TIMESTAMP        NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_product FOREIGN KEY (product_internal_id) REFERENCES products (internal_id) ON DELETE CASCADE
);

-- Triggers to update modified_at timestamp on parent product.
CREATE OR REPLACE FUNCTION update_product_modified_at()
    RETURNS TRIGGER AS
$$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        UPDATE products
        SET modified_at = NOW()
        WHERE internal_id = OLD.product_internal_id;
    ELSE
        UPDATE products
        SET modified_at = NOW()
        WHERE internal_id = NEW.product_internal_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS product_variants_update_modified_at ON product_variants;
CREATE TRIGGER product_variants_update_modified_at
    AFTER INSERT OR UPDATE OR DELETE
    ON product_variants
    FOR EACH ROW
EXECUTE FUNCTION update_product_modified_at();