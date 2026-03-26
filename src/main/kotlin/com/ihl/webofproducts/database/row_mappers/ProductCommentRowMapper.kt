package com.ihl.webofproducts.database.row_mappers

import com.ihl.webofproducts.database.model.ProductComment
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.util.UUID

class ProductCommentRowMapper(private val prefix: String = "") : RowMapper<ProductComment> {
    private fun col(name: String) = prefix + name

    override fun mapRow(rs: ResultSet, rowNum: Int): ProductComment {
        return ProductComment(
            productInternalId = rs.getObject(col("product_internal_id")) as UUID,
            variantInternalId = rs.getObject(col("variant_internal_id"), UUID::class.java),
            internalId = rs.getObject(col("internal_id")) as UUID,
            reviewerName = rs.getString(col("reviewer_name")),
            reviewerEmail = rs.getString(col("reviewer_email")),
            commentText = rs.getString(col("comment_text")),
            rating = rs.getInt(col("rating")),
            createdAt = rs.getTimestamp(col("created_at")).toLocalDateTime(),
            modifiedAt = rs.getTimestamp(col("modified_at")).toLocalDateTime(),
        )
    }
}
