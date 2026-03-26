package com.ihl.webofproducts.database.repository

import com.ihl.webofproducts.database.model.ActivityEvent
import com.ihl.webofproducts.database.model.Product
import com.ihl.webofproducts.database.model.ProductVariant
import com.ihl.webofproducts.database.row_mappers.ActivityEventRowMapper
import com.ihl.webofproducts.database.row_mappers.ProductRowMapper
import com.ihl.webofproducts.database.row_mappers.ProductVariantRowMapper
import com.ihl.webofproducts.service.FeedMetrics
import com.ihl.webofproducts.utils.PrefixedResultSetWrapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class ActivityEventRepository(
    private val jdbcClient: JdbcClient,
) {


    @Transactional
    fun save(event: ActivityEvent): ActivityEvent {
        val sql = """
            INSERT INTO catalog_activity_events (
                internal_id,
                product_internal_id,
                variant_internal_id,
                event_type,
                severity,
                source_label,
                headline,
                detail_text,
                metadata_json
            ) VALUES (
                :internal_id,
                :product_internal_id,
                :variant_internal_id,
                :event_type,
                :severity,
                :source_label,
                :headline,
                :detail_text,
                CAST(:metadata_json AS jsonb)
            )
            RETURNING *
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("internal_id", event.internalId)
            .param("product_internal_id", event.product!!.internalId)
            .param("variant_internal_id", event.variant!!.internalId)
            .param("event_type", event.eventType)
            .param("severity", event.severity)
            .param("source_label", event.sourceLabel)
            .param("headline", event.headline)
            .param("detail_text", event.detailText)
            .param("metadata_json", event.metadataJson)
            .query(ActivityEventRowMapper())
            .single()
    }

    fun fetchRecent(limit: Int = 12): List<ActivityEvent> {
        val sql = """
            SELECT *
            FROM catalog_activity_events
            ORDER BY created_at DESC
            LIMIT :limit
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("limit", limit)
            .query(ActivityEventRowMapper())
            .list()
    }

    fun fetchMetrics(): FeedMetrics {
        val sql = """
            SELECT
                (SELECT COUNT(*) FROM products) AS total_products,
                (SELECT COUNT(*) FROM product_variants) AS total_variants,
                (
                    SELECT COUNT(*)
                    FROM catalog_activity_events
                    WHERE created_at::date = CURRENT_DATE
                ) AS today_events,
                (
                    SELECT COUNT(*)
                    FROM catalog_activity_events
                    WHERE severity = 'critical'
                ) AS critical_events,
                (
                    SELECT headline
                    FROM catalog_activity_events
                    ORDER BY created_at DESC
                    LIMIT 1
                ) AS latest_headline,
                (
                    SELECT source_label
                    FROM catalog_activity_events
                    ORDER BY created_at DESC
                    LIMIT 1
                ) AS latest_source_label,
                (
                    SELECT created_at
                    FROM catalog_activity_events
                    ORDER BY created_at DESC
                    LIMIT 1
                ) AS last_event_at
        """.trimIndent()

        return jdbcClient.sql(sql)
            .query { rs, _ ->
                FeedMetrics(
                    productCount = rs.getInt("total_products"),
                    variantCount = rs.getInt("total_variants"),
                    todayEventCount = rs.getInt("today_events"),
                    criticalEventCount = rs.getInt("critical_events"),
                    latestHeadline = rs.getString("latest_headline"),
                    latestSourceLabel = rs.getString("latest_source_label"),
                    lastEventAtLabel = rs.getTimestamp("last_event_at")?.toLocalDateTime()?.toString(),
                )
            }
            .single()
    }

    fun fetchByInternalId(internalId: UUID): ActivityEvent? {
        val sql = """
            SELECT *
            FROM catalog_activity_events
            WHERE internal_id = :internal_id
        """.trimIndent()

        return jdbcClient.sql(sql)
            .param("internal_id", internalId)
            .query(ActivityEventRowMapper())
            .optional()
            .orElse(null)
    }

    fun fetchEventsAfterEvent(mainEvent: ActivityEvent): List<ActivityEvent> {
        return fetchWithRelationship(
            whereClause = """
                WHERE (e.created_at, e.internal_id) > (
                    SELECT created_at, internal_id
                    FROM catalog_activity_events
                    WHERE internal_id = :id
                )
            """.trimIndent(),
            params = mapOf("id" to mainEvent.internalId)
        )
    }

    fun fetchEventsAfterEvent(mainEventId: String): List<ActivityEvent> {
        return fetchWithRelationship(
            whereClause = """
                WHERE (e.created_at, e.internal_id) > (
                    SELECT created_at, internal_id
                    FROM catalog_activity_events
                    WHERE internal_id = :id
                )
            """.trimIndent(),
            params = mapOf("id" to java.util.UUID.fromString(mainEventId))
        )
    }

    fun fetchWithRelationship(
        whereClause: String = "",
        params: Map<String, Any> = emptyMap()
    ): List<ActivityEvent> {
        val sql = """
            SELECT 
                e.internal_id AS e_internal_id, e.product_internal_id AS e_product_internal_id, 
                e.variant_internal_id AS e_variant_internal_id, e.event_type as e_event_type, e.severity as e_severity,
                e.source_label as e_source_label, e.headline as e_headline, e.detail_text as e_detail_text, 
                e.metadata_json as e_metadata_json, e.created_at as e_created_at,
                p.internal_id AS p_internal_id, p.external_id AS p_external_id, p.title AS p_title, 
                p.body_html AS p_body_html, p.product_type AS p_product_type, p.image_url AS p_image_url, 
                p.created_at AS p_created_at, p.modified_at AS p_modified_at,
                v.product_internal_id AS v_product_internal_id, v.internal_id AS v_internal_id, 
                v.external_id AS v_external_id, v.sku as v_sku, v.title as v_title, v.price as v_price, v.color as v_color, 
                v.size as v_size, v.created_at as v_created_at, v.modified_at as v_modified_at, v.image_url as v_image_url
            FROM catalog_activity_events e
            LEFT JOIN products p ON  e.product_internal_id = p.internal_id
            LEFT JOIN product_variants v ON e.variant_internal_id = v.internal_id
            $whereClause
            ORDER BY e.created_at DESC
        """.trimIndent()

        val eventMap = mutableListOf<ActivityEvent>()
        var query = jdbcClient.sql(sql)
        params.forEach { (key, value) ->
            query = query.param(key, value)
        }
        query.query { rs, _ ->
            val eventRs = PrefixedResultSetWrapper(rs, "e_")
            val productRs = PrefixedResultSetWrapper(rs, "p_")
            val variantRs = PrefixedResultSetWrapper(rs, "v_")

            val event = ActivityEvent(
                internalId = eventRs.getObject("internal_id") as UUID,
                eventType = eventRs.getString("event_type") as String,
                severity = eventRs.getString("severity") as String,
                sourceLabel = eventRs.getString("source_label") as String,
                headline = eventRs.getString("headline") as String,
                detailText = eventRs.getString("detail_text") as String,
                metadataJson = eventRs.getString("metadata_json") as String,
                createdAt = eventRs.getTimestamp("created_at")!!.toLocalDateTime(),
            )

            var product: Product? = null
            val pId = rs.getObject("p_internal_id", UUID::class.java)
            if (pId != null) {
                val productMapper = ProductRowMapper()
                product = productMapper.mapRow(productRs, 0)
            }

            var variant: ProductVariant? = null
            val vId = rs.getObject("v_internal_id", UUID::class.java)
            if (vId != null) {
                val variantMapper = ProductVariantRowMapper()
                variant = variantMapper.mapRow(variantRs, 0)
            }


            val finalEvent = event.copy(
                product = product,
                variant = variant,
            )
            eventMap.add(finalEvent)
        }.list()

        return eventMap
    }

}
