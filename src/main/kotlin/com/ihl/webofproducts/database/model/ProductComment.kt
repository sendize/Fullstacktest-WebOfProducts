package com.ihl.webofproducts.database.model

import com.github.f4b6a3.uuid.UuidCreator
import java.time.LocalDateTime
import java.util.UUID

data class ProductComment(
    val productInternalId: UUID,
    val variantInternalId: UUID? = null,
    val internalId: UUID = UuidCreator.getTimeOrderedEpoch(),
    val reviewerName: String,
    val reviewerEmail: String,
    val commentText: String,
    val rating: Int,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val modifiedAt: LocalDateTime = LocalDateTime.now(),
)
