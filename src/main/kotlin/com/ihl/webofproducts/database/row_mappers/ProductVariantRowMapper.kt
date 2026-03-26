package com.ihl.webofproducts.database.row_mappers

import com.ihl.webofproducts.database.model.ProductVariant
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.util.UUID

class ProductVariantRowMapper(private val prefix: String = "") : RowMapper<ProductVariant> {
    private fun col(name: String) = prefix + name
    override fun mapRow(rs: ResultSet, rowNum: Int): ProductVariant {
        return ProductVariant(
            productInternalId = rs.getObject(col("product_internal_id")) as UUID?,
            internalId = rs.getObject(col("internal_id")) as UUID,
            externalId = rs.getLong(col("external_id")),
            sku = rs.getString(col("sku")),
            title = rs.getString(col("title")),
            price = rs.getDouble(col("price")),
            color = rs.getString(col("color")),
            size = rs.getString(col("size")),
            imageUrl = rs.getString(col("image_url")),
            createdAt = rs.getTimestamp(col("created_at")).toLocalDateTime(),
            modifiedAt = rs.getTimestamp(col("modified_at")).toLocalDateTime()
        )
    }
}