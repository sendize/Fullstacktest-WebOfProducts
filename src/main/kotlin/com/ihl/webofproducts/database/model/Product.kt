package com.ihl.webofproducts.database.model

import com.github.f4b6a3.uuid.UuidCreator
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

data class Product(
    val internalId: UUID = UuidCreator.getTimeOrderedEpoch(),
    val externalId: Long = Random.nextLong(),
    val title: String,
    val bodyHTML: String,
    val productType: String,
    val imageUrl: String,
    val variants: List<ProductVariant> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val modifiedAt: LocalDateTime = LocalDateTime.now(),
)
