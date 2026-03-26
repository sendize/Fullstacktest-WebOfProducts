package com.ihl.webofproducts.database.row_mappers

import com.ihl.webofproducts.database.model.ActivityEvent
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.util.UUID

class ActivityEventRowMapper : RowMapper<ActivityEvent> {
    override fun mapRow(rs: ResultSet, rowNum: Int): ActivityEvent =
        ActivityEvent(
            internalId = rs.getObject("internal_id") as UUID,
//            productInternalId = rs.getObject("product_internal_id", UUID::class.java),
//            variantInternalId = rs.getObject("variant_internal_id", UUID::class.java),
            eventType = rs.getString("event_type"),
            severity = rs.getString("severity"),
            sourceLabel = rs.getString("source_label"),
            headline = rs.getString("headline"),
            detailText = rs.getString("detail_text"),
            metadataJson = rs.getString("metadata_json"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
        )
}
