package com.ihl.webofproducts.database.repository

import com.ihl.webofproducts.database.model.Product
import com.ihl.webofproducts.database.model.ProductVariant
import com.ihl.webofproducts.database.row_mappers.ProductRowMapper
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement
import java.util.UUID

@Repository
class ProductRepository(
    private val jdbcClient: JdbcClient,
    private val jdbcTemplate: JdbcTemplate,
) {
    @Transactional
    fun save(product: Product): Product {
        val sql = """
            INSERT INTO products (internal_id, external_id, title, body_html, product_type, image_url) 
            VALUES (:internal_id, :external_id, :title, :body_html, :product_type, :image_url)
            RETURNING *""".trimIndent()
        val product = jdbcClient.sql(sql)
            .param("internal_id", product.internalId)
            .param("external_id", product.externalId)
            .param("title", product.title)
            .param("body_html", product.bodyHTML)
            .param("product_type", product.productType)
            .param("image_url", product.imageUrl)
            .query(ProductRowMapper())
            .single()

        return product
    }

    @Transactional
    fun update(product: Product): Product? {
        val sql = """
            UPDATE products 
            SET external_id = :external_id,
                title = :title,
                body_html = :body_html,
                product_type = :product_type,
                image_url = :image_url,
                modified_at = NOW()
            WHERE internal_id = :internal_id
            RETURNING *
        """.trimIndent()

        val product = jdbcClient.sql(sql)
            .param("internal_id", product.internalId)
            .param("external_id", product.externalId)
            .param("title", product.title)
            .param("body_html", product.bodyHTML)
            .param("product_type", product.productType)
            .param("image_url", product.imageUrl)
            .query(ProductRowMapper())
            .optional()
            .orElse(null)
        return product
    }

    @Transactional
    fun delete(id: Any): Boolean {
        val searchId = convertToUuid(id) ?: return false

        val sql = "DELETE FROM products WHERE internal_id = :id"

        val deletedRows = jdbcClient.sql(sql)
            .param("id", searchId)
            .update()

        return deletedRows > 0
    }

    fun fetchByInternalId(id: Any): Product? {
        val searchId = convertToUuid(id) ?: return null

        return fetchWithVariants(
            whereClause = "WHERE p.internal_id = :id",
            params = mapOf("id" to searchId)
        ).firstOrNull()
    }

    fun fetchWithVariants(
        whereClause: String = "",
        params: Map<String, Any> = emptyMap()
    ): List<Product> {
        val sql = """
            SELECT
                p.internal_id AS p_internal_id, p.external_id AS p_external_id, p.title AS p_title, 
                p.body_html AS p_body_html, p.product_type AS p_product_type, p.image_url AS p_image_url, 
                p.created_at AS p_created_at, p.modified_at AS p_modified_at,
                v.product_internal_id AS v_product_internal_id, v.internal_id AS v_internal_id, 
                v.external_id AS v_external_id, v.sku as v_sku, v.title as v_title, v.price as v_price, v.color as v_color, 
                v.size as v_size, v.created_at as v_created_at, v.modified_at as v_modified_at, v.image_url as v_image_url
            FROM products p
            LEFT JOIN product_variants v ON p.internal_id = v.product_internal_id
            $whereClause
            ORDER BY p.internal_id
            """.trimIndent()

        val productMap = mutableMapOf<java.util.UUID, Product>()
        val productVariantMap = mutableMapOf<java.util.UUID, MutableList<ProductVariant>>()

        var query = jdbcClient.sql(sql)
        params.forEach { (key, value) ->
            query = query.param(key, value)
        }
        query.query { rs, _ ->
            val pId = rs.getObject("p_internal_id") as java.util.UUID

            // Add product to map.
            if (!productMap.containsKey(pId)) {
                productMap[pId] = Product(
                    internalId = pId,
                    externalId = rs.getLong("p_external_id"),
                    title = rs.getString("p_title"),
                    bodyHTML = rs.getString("p_body_html"),
                    productType = rs.getString("p_product_type"),
                    imageUrl = rs.getString("p_image_url"),
                    createdAt = rs.getTimestamp("p_created_at").toLocalDateTime(),
                    modifiedAt = rs.getTimestamp("p_modified_at").toLocalDateTime(),
                    variants = emptyList()
                )
                productVariantMap[pId] = mutableListOf()
            }

            // Add variant to map.
            val vId = rs.getObject("v_internal_id", java.util.UUID::class.java)
            if (vId != null) {
                val variant = ProductVariant(
                    productInternalId = rs.getObject("v_product_internal_id") as java.util.UUID?,
                    internalId = vId,
                    externalId = rs.getLong("v_external_id"),
                    sku = rs.getString("v_sku"),
                    title = rs.getString("v_title"),
                    price = rs.getDouble("v_price"),
                    color = rs.getString("v_color"),
                    size = rs.getString("v_size"),
                    imageUrl = rs.getString("v_image_url"),
                    createdAt = rs.getTimestamp("v_created_at").toLocalDateTime(),
                    modifiedAt = rs.getTimestamp("v_modified_at").toLocalDateTime()
                )
                productVariantMap[pId]!!.add(variant)
            }
        }.list()

        // Combine products with their variants
        val res = productMap.map { (id, product) ->
            product.copy(variants = productVariantMap[id] ?: emptyList())
        }
        return res
    }

    fun getCount(): Int {
        val sql = """SELECT COUNT(*) FROM products""".trimIndent()
        val count = jdbcClient.sql(sql)
            .query(Int::class.java)
            .single()

        return count
    }

    @Transactional
    fun batchInsert(products: List<Product>) {
        batchInsertProducts(products)
        if (products.any { it.variants.isNotEmpty() }) {
            batchInsertVariants(products)
        }
    }

    private fun convertToUuid(id: Any): UUID? {
        val searchId = when (id) {
            is String -> java.util.UUID.fromString(id)
            is java.util.UUID -> id
            else -> return null
        }
        return searchId
    }

    private fun batchInsertProducts(products: List<Product>) {
        val sql = """
            INSERT INTO products (internal_id, external_id, title, body_html, product_type, image_url)
            VALUES (?, ?, ?, ?, ?, ?)""".trimIndent()

        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val product = products[i]
                ps.setObject(1, product.internalId)
                ps.setLong(2, product.externalId)
                ps.setString(3, product.title)
                ps.setString(4, product.bodyHTML)
                ps.setString(5, product.productType)
                ps.setString(6, product.imageUrl)
            }

            override fun getBatchSize(): Int = products.size
        })
    }

    private fun batchInsertVariants(products: List<Product>) {
        val allVariants = products.flatMap { product ->
            product.variants.map { variant ->
                Pair(product.internalId, variant)
            }
        }

        val sql = """
            INSERT INTO product_variants (product_internal_id, internal_id, external_id, sku, title, price, color, size, image_url) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimMargin()

        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val (productId, variant) = allVariants[i]
                ps.setObject(1, productId)
                ps.setObject(2, variant.internalId)
                ps.setLong(3, variant.externalId)
                ps.setString(4, variant.sku)
                ps.setString(5, variant.title)
                ps.setDouble(6, variant.price)
                ps.setString(7, variant.color)
                ps.setString(8, variant.size)
                ps.setString(9, variant.imageUrl)
            }

            override fun getBatchSize(): Int = allVariants.size
        })
    }
}