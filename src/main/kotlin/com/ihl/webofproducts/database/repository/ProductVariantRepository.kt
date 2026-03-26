package com.ihl.webofproducts.database.repository

import com.ihl.webofproducts.database.model.ProductVariant
import com.ihl.webofproducts.database.row_mappers.ProductVariantRowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class ProductVariantRepository(
    private val jdbcClient: JdbcClient,
) {

    @Transactional
    fun save(productVariant: ProductVariant): ProductVariant {
        val sql = """
            INSERT INTO product_variants (product_internal_id, internal_id, external_id, sku, title, price, color, size, image_url) 
            VALUES (:product_internal_id, :internal_id, :external_id, :sku, :title, :price, :color, :size, :image_url)
            RETURNING *
            """.trimMargin()
        val variant = jdbcClient.sql(sql)
            .param("product_internal_id", productVariant.productInternalId)
            .param("internal_id", productVariant.internalId)
            .param("external_id", productVariant.externalId)
            .param("sku", productVariant.sku)
            .param("title", productVariant.title)
            .param("price", productVariant.price)
            .param("color", productVariant.color)
            .param("size", productVariant.size)
            .param("image_url", productVariant.imageUrl)
            .query(ProductVariantRowMapper())
            .single()

        return variant
    }

    @Transactional
    fun update(productVariant: ProductVariant): ProductVariant? {
        val sql = """
            UPDATE product_variants 
            SET sku = :sku, 
                title = :title, 
                price = :price, 
                color = :color, 
                size = :size, 
                image_url = :image_url, 
                modified_at = NOW()
            WHERE internal_id = :internal_id
            RETURNING *
        """.trimIndent()

        val variant = jdbcClient.sql(sql)
            .param("internal_id", productVariant.internalId)
            .param("sku", productVariant.sku)
            .param("title", productVariant.title)
            .param("price", productVariant.price)
            .param("color", productVariant.color)
            .param("size", productVariant.size)
            .param("image_url", productVariant.imageUrl)
            .query(ProductVariantRowMapper())
            .optional()
            .orElse(null)

        return variant
    }

    @Transactional
    fun delete(id: Any): Boolean {
        val searchId = when (id) {
            is String -> UUID.fromString(id)
            is UUID -> id
            else -> return false
        }

        val sql = "DELETE FROM product_variants WHERE internal_id = :id"

        val deletedRows = jdbcClient.sql(sql)
            .param("id", searchId)
            .update()

        return deletedRows > 0
    }

    fun fetchByInternalId(id: Any): ProductVariant? {
        val searchId = when (id) {
            is String -> UUID.fromString(id)
            is UUID -> id
            else -> return null
        }

        val sql = "SELECT * FROM product_variants WHERE internal_id = :id"
        val variant = jdbcClient.sql(sql)
            .param("id", searchId)
            .query(ProductVariantRowMapper())
            .optional()
            .orElse(null)
        return variant
    }

    fun fetchAll(): List<ProductVariant> {
        val sql = "SELECT * FROM product_variants"
        val variant = jdbcClient.sql(sql)
            .query(ProductVariantRowMapper())
            .list()

        return variant
    }

    fun getCount(): Int {
        val sql = """SELECT COUNT(*) FROM product_variants""".trimIndent()
        val count = jdbcClient.sql(sql)
            .query(Int::class.java)
            .single()

        return count
    }
}