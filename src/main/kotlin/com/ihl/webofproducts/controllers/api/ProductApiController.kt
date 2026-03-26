package com.ihl.webofproducts.controllers.api


import com.ihl.webofproducts.database.model.Product
import com.ihl.webofproducts.database.repository.ProductRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/products")
class ProductApiController(
    private val productRepository: ProductRepository
) {
    @GetMapping
    fun getAllProducts(): ResponseEntity<List<Product>> {
        val products = productRepository.fetchWithVariants()
        return ResponseEntity.ok(products)
    }

    @GetMapping("/{id}")
    fun getProductById(@PathVariable id: String): ResponseEntity<Product> {
        val product = productRepository.fetchByInternalId(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(product)
    }

    @PostMapping
    fun createProduct(@RequestBody request: CreateProductRequest): ResponseEntity<Product> {
        val product = Product(
            title = request.title,
            bodyHTML = request.bodyHTML,
            productType = request.productType,
            imageUrl = request.imageUrl
        )
        val savedProduct = productRepository.save(product)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct)
    }

    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: String,
        @RequestBody request: UpdateProductRequest
    ): ResponseEntity<Product> {
        val existingProduct = productRepository.fetchByInternalId(id)
            ?: return ResponseEntity.notFound().build()

        val updatedProduct = existingProduct.copy(
            title = request.title ?: existingProduct.title,
            bodyHTML = request.bodyHTML ?: existingProduct.bodyHTML,
            productType = request.productType ?: existingProduct.productType,
            imageUrl = request.imageUrl ?: existingProduct.imageUrl
        )

        val savedProduct = productRepository.update(updatedProduct)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(savedProduct)
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = productRepository.delete(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

data class CreateProductRequest(
    val title: String,
    val bodyHTML: String,
    val productType: String,
    val imageUrl: String
)

data class UpdateProductRequest(
    val title: String? = null,
    val bodyHTML: String? = null,
    val productType: String? = null,
    val imageUrl: String? = null
)