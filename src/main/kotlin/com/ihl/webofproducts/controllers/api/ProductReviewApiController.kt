package com.ihl.webofproducts.controllers.api

import com.ihl.webofproducts.database.repository.ProductRepository
import com.ihl.webofproducts.service.ProductCommentForm
import com.ihl.webofproducts.service.ProductReviewPresentation
import com.ihl.webofproducts.service.ProductReviewService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products/{productId}/reviews")
class ProductReviewApiController(
    private val productRepository: ProductRepository,
    private val productReviewService: ProductReviewService,
) {

    @GetMapping
    fun getProductReviews(
        @PathVariable productId: String,
        @RequestParam(required = false, defaultValue = "rating-desc") sort: String,
    ): ResponseEntity<ProductReviewPresentation> {
        val product = productRepository.fetchByInternalId(productId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(productReviewService.buildPresentation(product, sort))
    }

    @PostMapping
    fun createProductReview(
        @PathVariable productId: String,
        @RequestBody request: CreateProductReviewRequest,
    ): ResponseEntity<Any> {
        val product = productRepository.fetchByInternalId(productId)
            ?: return ResponseEntity.notFound().build()

        return try {
            val savedReview = productReviewService.createReview(
                product = product,
                form = ProductCommentForm(
                    name = request.reviewerName,
                    email = request.reviewerEmail,
                    comment = request.commentText,
                    rating = request.rating,
                    variantId = request.variantInternalId,
                )
            )
            ResponseEntity.status(HttpStatus.CREATED).body(savedReview)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (ex.message ?: "Invalid review request.")))
        }
    }
}

data class CreateProductReviewRequest(
    val reviewerName: String,
    val reviewerEmail: String,
    val commentText: String,
    val rating: Int,
    val variantInternalId: String? = null,
)
