package com.ihl.webofproducts.database.model

import com.github.f4b6a3.uuid.UuidCreator
import com.ihl.webofproducts.service.FeedStreamEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID


data class ActivityEvent(
    val internalId: UUID = UuidCreator.getTimeOrderedEpoch(),
    val eventType: String,
    val severity: String,
    val sourceLabel: String,
    val headline: String,
    val detailText: String,
    val metadataJson: String = "{}",
    val createdAt: LocalDateTime = LocalDateTime.now(),

    val product: Product? = null,
    val variant: ProductVariant? = null,
) {
    fun toFeedEvent(): FeedStreamEvent {
        return FeedStreamEvent(
            eventType = eventType.replace('_', ' '),
            severity = severity,
            headline = headline,
            detailText = detailText,
            sourceLabel = sourceLabel,
            productLabel = product?.title,
            variantLabel = variant?.title,
            createdAtLabel = formatRelativeTime(createdAt),
        )
    }

    private val timeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)


    private fun formatRelativeTime(createdAt: LocalDateTime): String {
        val minutes = java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes()
        return when {
            minutes <= 0 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 1440 -> "${minutes / 60} hr ago"
            else -> timeFormatter.format(createdAt)
        }
    }
}

