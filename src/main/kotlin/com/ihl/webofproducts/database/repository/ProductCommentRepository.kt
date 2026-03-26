package com.ihl.webofproducts.database.repository

import com.ihl.webofproducts.database.model.ProductComment
import com.ihl.webofproducts.database.row_mappers.ProductCommentRowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

enum class ProductCommentSort(
    val value: String,
    private val orderByClause: String,
    val label: String,
) {
    RATING_DESC(
        value = "rating-desc",
        orderByClause = "ORDER BY rating DESC, created_at DESC",
        label = "Highest rated first",
    ),
    RATING_ASC(
        value = "rating-asc",
        orderByClause = "ORDER BY rating ASC, created_at DESC",
        label = "Lowest rated first",
    );

    fun orderByClause(): String = orderByClause

    companion object {
        fun fromValue(value: String?): ProductCommentSort =
            entries.firstOrNull { it.value == value } ?: RATING_DESC
    }
}

@Repository
class ProductCommentRepository(
    private val jdbcClient: JdbcClient,
) {
    @Transactional
    fun save(productComment: ProductComment): ProductComment {
        val sql = """
            INSERT INTO product_comments (
                internal_id,
                product_internal_id,
                variant_internal_id,
                reviewer_name,
                reviewer_email,
                comment_text,
                rating
            ) VALUES (
                :internal_id,
                :product_internal_id,
                :variant_internal_id,
                :reviewer_name,
                :reviewer_email,
                :comment_text,
                :rating
            )
            RETURNING *
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("internal_id", productComment.internalId)
            .param("product_internal_id", productComment.productInternalId)
            .param("variant_internal_id", productComment.variantInternalId)
            .param("reviewer_name", productComment.reviewerName)
            .param("reviewer_email", productComment.reviewerEmail)
            .param("comment_text", productComment.commentText)
            .param("rating", productComment.rating)
            .query(ProductCommentRowMapper())
            .single()
    }

    fun fetchByProductInternalId(
        productInternalId: UUID,
        sort: ProductCommentSort = ProductCommentSort.RATING_DESC,
    ): List<ProductComment> {
        val sql = """
            SELECT *
            FROM product_comments
            WHERE product_internal_id = :product_internal_id
            ${sort.orderByClause()}
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("product_internal_id", productInternalId)
            .query(ProductCommentRowMapper())
            .list()
    }
}
