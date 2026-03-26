package com.ihl.webofproducts.controllers.web

import com.ihl.webofproducts.database.model.Product
import com.ihl.webofproducts.database.model.ProductVariant
import com.ihl.webofproducts.database.repository.ProductRepository
import com.ihl.webofproducts.service.ProductCommentForm
import com.ihl.webofproducts.service.ProductReviewService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.text.NumberFormat
import java.util.Locale

@Controller
class ProductPageController(
    private val productRepository: ProductRepository,
    private val productReviewService: ProductReviewService,
) {

    private val usdCurrencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    @ModelAttribute("products")
    fun products(): MutableList<Product> = mutableListOf()

    @ModelAttribute("theme")
    fun theme(): String = "light"

    @GetMapping("/")
    fun index(): String = "index"

    @GetMapping("/products/showcase")
    fun productShowcase(
        @RequestParam(required = false) productId: String?,
        @RequestParam(required = false) variantId: String?,
        @RequestParam(required = false, defaultValue = "rating-desc") sort: String,
        model: Model,
    ): String {
        val catalog = loadCatalog()
        val featuredProduct = resolveProduct(catalog, productId)
        val selectedVariant = resolveVariant(featuredProduct, variantId)
        populateShowcaseModel(model, catalog, featuredProduct, selectedVariant, sort)
        return "product-page"
    }

    @GetMapping("/products/showcase/{id}")
    fun productShowcaseById(
        @PathVariable id: String,
        @RequestParam(required = false) variantId: String?,
        @RequestParam(required = false, defaultValue = "rating-desc") sort: String,
        model: Model,
    ): String {
        val catalog = loadCatalog()
        val featuredProduct = resolveProduct(catalog, id)
        val selectedVariant = resolveVariant(featuredProduct, variantId)
        populateShowcaseModel(model, catalog, featuredProduct, selectedVariant, sort)
        return "product-page"
    }

    @GetMapping("/products/showcase/{id}/experience")
    fun productShowcaseExperience(
        @PathVariable id: String,
        @RequestParam(required = false) variantId: String?,
        @RequestParam(required = false, defaultValue = "rating-desc") sort: String,
        model: Model,
    ): String {
        val catalog = loadCatalog()
        val featuredProduct = resolveProduct(catalog, id)
        val selectedVariant = resolveVariant(featuredProduct, variantId)
        populateShowcaseModel(model, catalog, featuredProduct, selectedVariant, sort)
        return "product-page :: showcaseContent"
    }

    @GetMapping("/products/showcase/{id}/reviews")
    fun productReviews(
        @PathVariable id: String,
        @RequestParam(required = false) variantId: String?,
        @RequestParam(required = false, defaultValue = "rating-desc") sort: String,
        model: Model,
    ): String {
        val catalog = loadCatalog()
        val featuredProduct = resolveProduct(catalog, id)
        val selectedVariant = resolveVariant(featuredProduct, variantId)
        populateShowcaseModel(model, catalog, featuredProduct, selectedVariant, sort)
        return "product-page :: reviewsSection"
    }

    @PostMapping("/products/showcase/{id}/reviews")
    fun submitProductReview(
        @PathVariable id: String,
        @RequestParam name: String,
        @RequestParam email: String,
        @RequestParam comment: String,
        @RequestParam rating: Int,
        @RequestParam(required = false) variantId: String?,
        @RequestParam(required = false) selectedVariantId: String?,
        @RequestParam(required = false, defaultValue = "rating-desc") sort: String,
        model: Model,
    ): String {
        val catalog = loadCatalog()
        val featuredProduct = resolveProduct(catalog, id)
        val selectedVariant = resolveVariant(featuredProduct, selectedVariantId)
        val submissionResult = productReviewService.submitReview(
            featuredProduct,
            ProductCommentForm(
                name = name,
                email = email,
                comment = comment,
                rating = rating,
                variantId = variantId,
            )
        )

        populateShowcaseModel(
            model = model,
            catalog = catalog,
            featuredProduct = featuredProduct,
            selectedVariant = selectedVariant,
            sort = sort,
            commentForm = submissionResult.form,
            commentSubmissionNotice = submissionResult.notice,
            commentNoticeIsError = submissionResult.isError,
        )
        return "product-page :: reviewsSection"
    }

    @GetMapping("/products/load")
    fun loadProducts(model: Model): String {

        val products = productRepository.fetchWithVariants()
        model.addAttribute("products", products)
        return "fragments/product-table :: productRows"
    }

    @PostMapping("/products/add")
    fun addProduct(
        @RequestParam productTitle: String,
        @RequestParam productDesc: String,
        @RequestParam productType: String,
        @RequestParam imageUrl: String,
        model: Model,
    ): String {
        val product = Product(
            title = productTitle,
            bodyHTML = productDesc,
            productType = productType,
            imageUrl = imageUrl
        )

        productRepository.save(product)
        val products = productRepository.fetchWithVariants()
        model.addAttribute("products", products)
        return "fragments/product-table :: productRows"
    }

    @PostMapping("/theme/toggle")
    fun toggleTheme(@ModelAttribute("theme") theme: String, model: Model): String {
        val nextTheme = if (theme == "dark") "light" else "dark"
        model.addAttribute("theme", nextTheme)
        return "index :: appRoot"
    }

    private fun populateShowcaseModel(
        model: Model,
        catalog: List<Product>,
        featuredProduct: Product,
        selectedVariant: ProductVariant?,
        sort: String,
        commentForm: ProductCommentForm = ProductCommentForm(),
        commentSubmissionNotice: String? = null,
        commentNoticeIsError: Boolean = false,
    ) {
        val featureChips = buildFeatureChips(featuredProduct, selectedVariant)
        val originalPrice = selectedVariant?.price?.times(1.18)
        val priceLabel = selectedVariant?.price?.let(usdCurrencyFormat::format) ?: "Made to order"
        val heroImageUrl = selectedVariant?.imageUrl?.takeIf { it.isNotBlank() }
            ?: featuredProduct.imageUrl
        val relatedProducts = catalog.filter { it.internalId != featuredProduct.internalId }
        val reviewPresentation = productReviewService.buildPresentation(featuredProduct, sort)

        model.addAttribute("catalogProducts", catalog)
        model.addAttribute("featuredProduct", featuredProduct)
        model.addAttribute("selectedVariant", selectedVariant)
        model.addAttribute("variantOptions", featuredProduct.variants.filter { it.imageUrl.isNotBlank() })
        model.addAttribute("reviewVariantOptions", featuredProduct.variants)
        model.addAttribute("heroImageUrl", heroImageUrl)
        model.addAttribute("priceLabel", priceLabel)
        model.addAttribute("originalPriceLabel", originalPrice?.let(usdCurrencyFormat::format))
        model.addAttribute("featuredBadge", if (selectedVariant != null) "Variant Spotlight" else "Catalog Pick")
        model.addAttribute(
            "availabilityLabel",
            if (featuredProduct.variants.isEmpty()) "Preview available" else "In stock for dispatch"
        )
        model.addAttribute(
            "shippingLabel",
            if (selectedVariant != null) "Ships in 24 hours" else "Ships in 2-3 business days"
        )
        model.addAttribute("featureChips", featureChips)
        model.addAttribute("ratingLabel", reviewPresentation.ratingLabel)
        model.addAttribute("reviewLabel", reviewPresentation.reviewLabel)
        model.addAttribute("variantCountLabel", "${featuredProduct.variants.size.coerceAtLeast(1)} option(s)")
        model.addAttribute("heroMediaLabel", if (selectedVariant != null) "Variant image" else "Product image")
        model.addAttribute(
            "fallbackModeLabel",
            if (catalog.first().externalId == -1L) "Demo fallback" else "Live catalog"
        )
        model.addAttribute(
            "variantSpecBody",
            if (featuredProduct.variants.isEmpty()) {
                "${featuredProduct.title} currently has no stored variants, so the page is rendering its base configuration."
            } else {
                "${featuredProduct.title} exposes ${featuredProduct.variants.size} variant option(s), each selectable through an HTMX fragment request."
            }
        )
        model.addAttribute(
            "priceSpecBody",
            selectedVariant?.let {
                "${it.title} is currently selected, so the pricing card is showing the variant-specific amount and comparison price."
            } ?: "No variant override is selected, so the page is presenting the product as a made-to-order base item."
        )
        model.addAttribute(
            "mediaSpecBody",
            selectedVariant?.let {
                "The hero image now reflects ${it.title}, using the variant media while keeping the rest of the page in sync."
            }
                ?: "The hero image is currently using the product-level media because there is no active variant image override."
        )
        model.addAttribute(
            "fallbackSpecBody",
            if (catalog.first().externalId == -1L) {
                "The live catalog is empty, so a built-in demo product is keeping the route and HTMX fragments renderable."
            } else {
                "This page is using live repository data, and all fragment swaps are resolved against the persisted catalog."
            }
        )
        model.addAttribute("defaultVariantLabel", featuredProduct.variants.firstOrNull()?.title ?: "Base configuration")
        model.addAttribute("relatedProducts", relatedProducts.take(3))
        model.addAttribute("activeSort", reviewPresentation.activeSort)
        model.addAttribute("activeSortLabel", reviewPresentation.activeSortLabel)
        model.addAttribute("commentCountLabel", reviewPresentation.commentCountLabel)
        model.addAttribute("reviewSummary", reviewPresentation.reviewSummary)
        model.addAttribute("fiveStarShare", reviewPresentation.fiveStarShare)
        model.addAttribute("fourStarShare", reviewPresentation.fourStarShare)
        model.addAttribute("threeStarAndBelowShare", reviewPresentation.threeStarAndBelowShare)
        model.addAttribute("productComments", reviewPresentation.productComments)
        model.addAttribute("commentForm", commentForm)
        model.addAttribute("commentSubmissionNotice", commentSubmissionNotice)
        model.addAttribute("commentNoticeIsError", commentNoticeIsError)
    }

    private fun buildFeatureChips(
        featuredProduct: Product,
        selectedVariant: ProductVariant?,
    ): List<String> {
        val chips = mutableListOf<String>()
        chips += featuredProduct.productType
        selectedVariant?.color?.takeIf { it.isNotBlank() }?.let(chips::add)
        selectedVariant?.size?.takeIf { it.isNotBlank() }?.let(chips::add)
        if (featuredProduct.variants.isNotEmpty()) {
            chips += "${featuredProduct.variants.size} variants"
        }
        chips += "HTMX ready"
        return chips.distinct().take(4)
    }

    private fun loadCatalog(): List<Product> {
        val catalog = productRepository.fetchWithVariants()
        return if (catalog.isNotEmpty()) catalog else listOf(buildFallbackProduct())
    }

    private fun resolveProduct(catalog: List<Product>, productId: String?): Product {
        if (productId.isNullOrBlank()) {
            return catalog.first()
        }
        return catalog.firstOrNull { it.internalId.toString() == productId } ?: catalog.first()
    }

    private fun resolveVariant(product: Product, variantId: String?): ProductVariant? {
        if (product.variants.isEmpty()) {
            return null
        }
        if (variantId.isNullOrBlank()) {
            return product.variants.first()
        }
        return product.variants.firstOrNull { it.internalId.toString() == variantId } ?: product.variants.first()
    }

    private fun buildFallbackProduct(): Product =
        Product(
            externalId = -1,
            title = "Auralith One Mechanical Keyboard",
            bodyHTML = "A premium showcase-ready product used when the live catalog is empty. It keeps the page renderable while the inventory is still loading.",
            productType = "Mechanical Keyboard",
            imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=1200&q=80",
            variants = listOf(
                ProductVariant(
                    productInternalId = null,
                    externalId = -11,
                    sku = "AURALITH-GRAPHITE",
                    title = "Graphite / Tactile",
                    price = 189.0,
                    color = "Graphite",
                    size = "84-key",
                    imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=1200&q=80",
                ),
                ProductVariant(
                    productInternalId = null,
                    externalId = -12,
                    sku = "AURALITH-SAND",
                    title = "Sand / Linear",
                    price = 199.0,
                    color = "Sand",
                    size = "84-key",
                    imageUrl = "https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?auto=format&fit=crop&w=1200&q=80",
                ),
            ),
        )
}
