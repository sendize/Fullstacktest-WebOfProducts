package com.ihl.webofproducts.controllers.api

import com.ihl.webofproducts.database.model.ProductVariant
import com.ihl.webofproducts.database.repository.ProductRepository
import com.ihl.webofproducts.database.repository.ProductVariantRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/variants")
class ProductVariantApiController(
    private val productVariantRepository: ProductVariantRepository,
    private val productRepository: ProductRepository
) {

    @GetMapping
    fun getAllVariants(): ResponseEntity<List<ProductVariant>> {
        val variants = productVariantRepository.fetchAll()
        return ResponseEntity.ok(variants)
    }

    @GetMapping("/{id}")
    fun getVariantById(@PathVariable id: String): ResponseEntity<ProductVariant> {
        val variant = productVariantRepository.fetchByInternalId(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(variant)
    }

    @PostMapping
    fun createVariant(@RequestBody request: CreateProductVariantRequest): ResponseEntity<Any> {
        // Verify the product exists
        val product = productRepository.fetchByInternalId(request.productInternalId)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Product with id ${request.productInternalId} not found"))

        val variant = ProductVariant(
            productInternalId = product.internalId,
            sku = request.sku,
            title = request.title,
            price = request.price,
            color = request.color,
            size = request.size,
            imageUrl = request.imageUrl
        )
        val savedVariant = productVariantRepository.save(variant)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedVariant)
    }

    @PutMapping("/{id}")
    fun updateVariant(
        @PathVariable id: String,
        @RequestBody request: UpdateProductVariantRequest
    ): ResponseEntity<ProductVariant> {
        val existingVariant = productVariantRepository.fetchByInternalId(id)
            ?: return ResponseEntity.notFound().build()

        val updatedVariant = existingVariant.copy(
            sku = request.sku ?: existingVariant.sku,
            title = request.title ?: existingVariant.title,
            price = request.price ?: existingVariant.price,
            color = request.color ?: existingVariant.color,
            size = request.size ?: existingVariant.size,
            imageUrl = request.imageUrl ?: existingVariant.imageUrl
        )

        val savedVariant = productVariantRepository.update(updatedVariant)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(savedVariant)
    }

    @DeleteMapping("/{id}")
    fun deleteVariant(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = productVariantRepository.delete(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

data class CreateProductVariantRequest(
    val productInternalId: UUID,
    val sku: String,
    val title: String,
    val price: Double,
    val color: String,
    val size: String,
    val imageUrl: String
)

data class UpdateProductVariantRequest(
    val sku: String? = null,
    val title: String? = null,
    val price: Double? = null,
    val color: String? = null,
    val size: String? = null,
    val imageUrl: String? = null
)