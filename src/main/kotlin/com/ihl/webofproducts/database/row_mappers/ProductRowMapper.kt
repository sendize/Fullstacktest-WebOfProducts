package com.ihl.webofproducts.database.row_mappers

import com.ihl.webofproducts.database.model.Product
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.util.UUID

class ProductRowMapper(private val prefix: String = "") : RowMapper<Product> {
    private fun col(name: String) = prefix + name
    override fun mapRow(rs: ResultSet, rowNum: Int): Product {
        return Product(
            internalId = rs.getObject(col("internal_id")) as UUID,
            externalId = rs.getLong(col("external_id")),
            title = rs.getString(col("title")),
            bodyHTML = rs.getString(col("body_html")),
            productType = rs.getString(col("product_type")),
            imageUrl = rs.getString(col("image_url")),
            createdAt = rs.getTimestamp(col("created_at")).toLocalDateTime(),
            modifiedAt = rs.getTimestamp(col("modified_at")).toLocalDateTime()
        )
    }
}