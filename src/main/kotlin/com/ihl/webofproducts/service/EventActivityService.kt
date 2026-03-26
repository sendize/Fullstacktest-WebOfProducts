package com.ihl.webofproducts.service

import com.ihl.webofproducts.database.model.ActivityEvent
import com.ihl.webofproducts.database.model.Product
import com.ihl.webofproducts.database.model.ProductVariant
import com.ihl.webofproducts.database.repository.ActivityEventRepository
import com.ihl.webofproducts.database.repository.ProductRepository
import com.ihl.webofproducts.exception.EmitterException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.context.Context
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2

data class FeedMetrics(
    val productCount: Int,
    val variantCount: Int,
    val todayEventCount: Int,
    val criticalEventCount: Int,
    val latestHeadline: String? = null,
    val latestSourceLabel: String? = null,
    val lastEventAtLabel: String? = null,
)

data class FeedStreamEvent(
    val eventType: String,
    val severity: String,
    val headline: String,
    val detailText: String,
    val sourceLabel: String,
    val productLabel: String?,
    val variantLabel: String?,
    val createdAtLabel: String,
)

data class FormOptions(
    val value: String,
    val label: String,
)

data class ClientConnection(
    val emitter: SseEmitter,
    var lastEventId: String?
)

@Service
class CatalogPulseService(
    private val activityEventRepository: ActivityEventRepository,
    private val productRepository: ProductRepository,
    private val templateEngine: ITemplateEngine,
) {
    private val feedEmitterMap = ConcurrentHashMap<String, ClientConnection>()

    fun subscribeToFeedEmitter(): SseEmitter {
        val emitter = SseEmitter(10 * 60 * 1000L)
        val emitterId = UUID.randomUUID().toString()

        val conn = ClientConnection(
            emitter = emitter,
            lastEventId = null
        )

        feedEmitterMap[emitterId] = conn
        emitter.onCompletion {
            feedEmitterMap.remove(emitterId)
            println("emitter complete: $emitterId")
        }
        emitter.onTimeout {
            feedEmitterMap.remove(emitterId)
            emitter.complete()
            println("emitter timeout: $emitterId")
        }
        emitter.onError {
            feedEmitterMap.remove(emitterId)
            emitter.complete()
            println("emitter error: $emitterId")
        }

        val events = activityEventRepository.fetchWithRelationship()
        val mostRecentEvent = events.first()
        onEventUpdate(mostRecentEvent, emitterId)

        return emitter
    }

    fun createEvent(
        eventType: String,
        severity: String,
        sourceLabel: String,
        headline: String,
        detailText: String,
        metadataJson: String,
        productId: String?,
        variantId: String?,
    ): ActivityEvent {
        val newEvent = ActivityEvent(
            eventType = eventType,
            severity = severity,
            sourceLabel = sourceLabel,
            headline = headline,
            detailText = detailText,
            metadataJson = metadataJson,
            product = productId?.let {
                Product(
                    internalId = UUID.fromString(it),
                    title = "",
                    bodyHTML = "",
                    productType = "",
                    imageUrl = "",
                    variants = emptyList(),
                )
            },
            variant = variantId?.let {
                ProductVariant(
                    internalId = UUID.fromString(it),
                    sku = "",
                    title = "",
                    price = 0.0,
                    color = "",
                    size = "",
                    imageUrl = ""
                )
            },
        )

        val newEventFromDb = activityEventRepository.save(newEvent)
        onEventUpdate(newEventFromDb)
        return newEventFromDb
    }

    fun recentEvents(limit: Int = 12): List<ActivityEvent> =
        activityEventRepository.fetchRecent(limit)

    fun getEventProductAndVariantOptions(): Pair<List<FormOptions>, List<FormOptions>> {
        val products = productRepository.fetchWithVariants()
        val variants = products.flatMap { it.variants }
        val productById = products.associateBy { it.internalId }

        val productOptions = products.map { FormOptions(it.internalId.toString(), it.title) }
        val variantsOptions = variants.map { variant ->
            val productTitle = variant.productInternalId?.let(productById::get)?.title
            FormOptions(
                value = variant.internalId.toString(),
                label = if (productTitle == null) variant.title else "$productTitle · ${variant.title}",
            )
        }
        return Pair(productOptions, variantsOptions)
    }

    fun getEventSeverityOptions(): List<FormOptions> {
        val severities = listOf(
            FormOptions("info", "Info"),
            FormOptions("success", "Success"),
            FormOptions("warning", "Warning"),
            FormOptions("critical", "Critical"),
        )

        return severities
    }

    fun getEventTypeOptions(): List<FormOptions> {
        val eventTypes = listOf(
            FormOptions("PRODUCT_SYNC", "Product Sync"),
            FormOptions("SHOWCASE_UPDATE", "Showcase Update"),
            FormOptions("PRICE_ALERT", "Price Alert"),
            FormOptions("IMPORT_STATUS", "Import Status"),
            FormOptions("MERCH_ALERT", "Merch Alert"),
        )

        return eventTypes
    }

    private fun renderFragment(fragmentLink: String, variables: Map<String, Any?>): String {
        println(variables)
        val context = Context(Locale.US).apply {
            variables.forEach { (key, value) ->
                setVariable(key, value)
            }
        }

        return templateEngine.process(fragmentLink, context)
    }

    @Scheduled(fixedDelay = 5000)
    private fun scheduledEventStreamUpdate() {
        val lastEvent = activityEventRepository.fetchRecent(1).last()
        onEventUpdate(lastEvent)
    }

    fun onEventUpdate(event: ActivityEvent, specificEmitterId: String? = null) {
        val staleEmitterIds = mutableListOf<String>()

        val metrics = activityEventRepository.fetchMetrics()
        val allEvents = activityEventRepository.fetchWithRelationship()
        for (connObject in feedEmitterMap) {
            val (id, conn) = connObject

            if (specificEmitterId != null && id != specificEmitterId) {
                continue
            }

            if (conn.lastEventId == event.internalId.toString()) {
                continue
            }

            var newEvents: List<ActivityEvent>
            if (conn.lastEventId == null) {
                newEvents = allEvents
            } else {
                newEvents = activityEventRepository.fetchEventsAfterEvent(conn.lastEventId!!)
                if (newEvents.isEmpty()) {
                    // Possible when the latest one has been deleted.
                    newEvents = allEvents
                }
            }

            val feedEvents = newEvents.map { it.toFeedEvent() }
            val lastEventId = newEvents.first().internalId.toString()
            try {
                sendEmitterEvent(
                    conn.emitter,
                    renderFragment(
                        "fragments/sse/feed-stream-item",
                        mapOf("events" to feedEvents)
                    ),
                    lastEventId
                )

                sendEmitterEvent(
                    conn.emitter,
                    mapOf(
                        "productCount" to metrics.productCount,
                        "variantCount" to metrics.variantCount,
                        "todayEventCount" to metrics.todayEventCount,
                        "criticalEventCount" to metrics.criticalEventCount,
                    ),
                    "metricsChange"
                )

                conn.lastEventId = newEvents.first().internalId.toString()
            } catch (e: EmitterException) {
                staleEmitterIds += id
            }
        }

        staleEmitterIds.forEach { id ->
            feedEmitterMap.remove(id)?.emitter?.complete()
        }
    }

    private fun sendEmitterEvent(emitter: SseEmitter, renderFragment: String, id: String?, eventName: String? = null) {
        try {
            val event = SseEmitter.event().data(renderFragment)
            if (id != null) {
                event.id(id)
            }
            if (eventName != null) {
                event.name(eventName)
            }
            emitter.send(event)
        } catch (e: Exception) {
            throw EmitterException(e)
        }
    }

    private fun sendEmitterEvent(emitter: SseEmitter, dataMap: Map<Any, Any>, eventName: String) {
        try {
            val event = SseEmitter.event()
                .name(eventName)
                .data(dataMap)
            emitter.send(event)
        } catch (e: Exception) {
            throw EmitterException(e)
        }
    }
}


