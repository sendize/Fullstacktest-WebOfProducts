package com.ihl.webofproducts.database.model


import com.github.f4b6a3.uuid.UuidCreator
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

data class ProductVariant(
    val productInternalId: UUID? = null,

    val internalId: UUID = UuidCreator.getTimeOrderedEpoch(),
    val externalId: Long = Random.nextLong(),
    val sku: String,
    val title: String,
    val price: Double,
    val color: String,
    val size: String,
    val imageUrl: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val modifiedAt: LocalDateTime = LocalDateTime.now(),
)