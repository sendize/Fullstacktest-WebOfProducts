package com.ihl.webofproducts.service

import com.ihl.webofproducts.database.model.Product
import com.ihl.webofproducts.database.model.ProductComment
import com.ihl.webofproducts.database.repository.ProductCommentSort
import com.ihl.webofproducts.database.repository.ProductCommentRepository
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

enum class ReviewSort(
    val value: String,
    val label: String,
    val highestFirst: Boolean,
) {
    RATING_DESC("rating-desc", "Highest rated first", true),
    RATING_ASC("rating-asc", "Lowest rated first", false);

    companion object {
        fun from(value: String?): ReviewSort = when (value?.lowercase(Locale.US)) {
            "lowest", "rating-asc" -> RATING_ASC
            else -> RATING_DESC
        }
    }
}

data class ProductCommentForm(
    var name: String = "",
    var email: String = "",
    var comment: String = "",
    var rating: Int? = 5,
    var variantId: String? = null,
)

data class ProductCommentView(
    val name: String,
    val email: String,
    val comment: String,
    val rating: Int,
    val variantTitle: String?,
    val createdAtLabel: String,
)

data class ProductReviewPresentation(
    val activeSort: String,
    val activeSortLabel: String,
    val ratingLabel: String,
    val reviewLabel: String,
    val commentCountLabel: String,
    val reviewSummary: String,
    val fiveStarShare: Int,
    val fourStarShare: Int,
    val threeStarAndBelowShare: Int,
    val productComments: List<ProductCommentView>,
)

data class ProductReviewSubmissionResult(
    val notice: String,
    val form: ProductCommentForm,
    val isError: Boolean,
)

@Service
class ProductReviewService(
    private val productCommentRepository: ProductCommentRepository,
) {
    private val ratingFormat = DecimalFormat("0.0")
    private val commentDateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun buildPresentation(
        product: Product,
        sortValue: String?,
    ): ProductReviewPresentation {
        val sort = ReviewSort.from(sortValue)
        val comments = productCommentRepository.fetchByProductInternalId(
            product.internalId,
            ProductCommentSort.fromValue(sort.value),
        )
        val averageRating = comments.map(ProductComment::rating).average().takeUnless { it.isNaN() }
        val totalComments = comments.size
        val variantTitleById = product.variants.associateBy({ it.internalId }, { it.title })

        val productComments = comments.map { comment ->
            ProductCommentView(
                name = comment.reviewerName,
                email = comment.reviewerEmail,
                comment = comment.commentText,
                rating = comment.rating,
                variantTitle = comment.variantInternalId?.let(variantTitleById::get),
                createdAtLabel = comment.createdAt.format(commentDateFormat),
            )
        }

        return ProductReviewPresentation(
            activeSort = sort.value,
            activeSortLabel = sort.label,
            ratingLabel = averageRating?.let(ratingFormat::format) ?: "0.0",
            reviewLabel = reviewLabel(totalComments),
            commentCountLabel = commentLabel(totalComments),
            reviewSummary = buildSummary(product.title, averageRating, totalComments),
            fiveStarShare = ratingShare(comments, totalComments) { it == 5 },
            fourStarShare = ratingShare(comments, totalComments) { it == 4 },
            threeStarAndBelowShare = ratingShare(comments, totalComments) { it <= 3 },
            productComments = productComments,
        )
    }

    fun submitReview(
        product: Product,
        form: ProductCommentForm,
    ): ProductReviewSubmissionResult {
        val normalizedForm = form.copy(
            name = form.name.trim(),
            email = form.email.trim(),
            comment = form.comment.trim(),
            variantId = form.variantId?.trim().orEmpty(),
        )

        return try {
            createReview(product, normalizedForm)
            ProductReviewSubmissionResult(
                notice = "Thanks, ${normalizedForm.name}. Your rating was saved.",
                form = ProductCommentForm(rating = 5),
                isError = false,
            )
        } catch (ex: IllegalArgumentException) {
            ProductReviewSubmissionResult(
                notice = ex.message ?: "Unable to save your rating right now.",
                form = normalizedForm,
                isError = true,
            )
        }
    }

    fun createReview(
        product: Product,
        form: ProductCommentForm,
    ): ProductComment {
        val normalizedName = form.name.trim()
        val normalizedEmail = form.email.trim()
        val normalizedComment = form.comment.trim()
        val normalizedRating = form.rating?.coerceIn(1, 5)
            ?: throw IllegalArgumentException("Rating is required.")
        val selectedVariantId = parseVariantId(form.variantId)

        require(normalizedName.isNotBlank()) { "Name is required." }
        require(normalizedEmail.isNotBlank()) { "Email is required." }
        require(emailPattern.matches(normalizedEmail)) { "Email address is invalid." }
        require(normalizedComment.isNotBlank()) { "Comment is required." }
        if (selectedVariantId != null && product.variants.none { it.internalId == selectedVariantId }) {
            throw IllegalArgumentException("Selected variant does not belong to this product.")
        }

        return productCommentRepository.save(
            ProductComment(
                productInternalId = product.internalId,
                variantInternalId = selectedVariantId,
                reviewerName = normalizedName,
                reviewerEmail = normalizedEmail,
                commentText = normalizedComment,
                rating = normalizedRating,
            )
        )
    }

    private fun buildSummary(
        productTitle: String,
        averageRating: Double?,
        totalComments: Int,
    ): String {
        if (totalComments == 0) {
            return "No comments have been saved for ${productTitle.lowercase()} yet. Be the first to share a rating."
        }

        return when {
            averageRating == null -> "Comments are available for ${productTitle.lowercase()}."
            averageRating >= 4.5 -> "Recent buyers consistently rate ${productTitle.lowercase()} highly for its overall experience."
            averageRating >= 4.0 -> "Most buyers report a strong experience with ${productTitle.lowercase()} and dependable quality."
            averageRating >= 3.0 -> "Comments on ${productTitle.lowercase()} are mixed, with balanced feedback across ratings."
            else -> "Recent comments point to room for improvement across the ${productTitle.lowercase()} experience."
        }
    }

    private fun ratingShare(
        comments: List<ProductComment>,
        totalComments: Int,
        predicate: (Int) -> Boolean,
    ): Int {
        if (totalComments == 0) {
            return 0
        }
        val matchingRatings = comments.count { predicate(it.rating) }
        return ((matchingRatings.toDouble() / totalComments.toDouble()) * 100).toInt()
    }

    private fun commentLabel(totalComments: Int): String =
        if (totalComments == 1) "1 comment" else "$totalComments comments"

    private fun reviewLabel(totalComments: Int): String =
        if (totalComments == 1) "1 review" else "$totalComments reviews"

    private fun parseVariantId(rawVariantId: String?): UUID? {
        if (rawVariantId.isNullOrBlank()) {
            return null
        }
        return runCatching { UUID.fromString(rawVariantId) }.getOrNull()
    }
}
