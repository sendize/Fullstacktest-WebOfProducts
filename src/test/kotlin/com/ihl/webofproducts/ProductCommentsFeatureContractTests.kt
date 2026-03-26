package com.ihl.webofproducts

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

class ProductCommentsFeatureContractTests {

    private val projectRoot: Path = Path.of("").toAbsolutePath().normalize()
    private val controllersRoot = projectRoot.resolve("src/main/kotlin/com/ihl/webofproducts/controllers")
    private val repositoriesRoot = projectRoot.resolve("src/main/kotlin/com/ihl/webofproducts/database/repository")
    private val servicesRoot = projectRoot.resolve("src/main/kotlin/com/ihl/webofproducts/service")
    private val templatePath = projectRoot.resolve("src/main/resources/templates/product-page.html")
    private val httpTestsRoot = projectRoot.resolve("src/test/http")

    @Test
    fun `product page template renders a real comment form and sort controls`() {
        val template = templatePath.readText()

        assertContainsAll(
            template,
            listOf(
                "id=\"reviews\"",
                "name=\"name\"",
                "name=\"email\"",
                "name=\"comment\"",
                "name=\"rating\"",
            ),
            "Expected product page template to render the product comment form fields."
        )

        assertTrue(
            template.contains("variant", ignoreCase = true),
            "Expected product page template to expose optional variant selection for a product comment."
        )
        assertTrue(
            template.contains("sort", ignoreCase = true),
            "Expected product page template to expose a rating sort control."
        )
        assertTrue(
            template.contains("review-card") || template.contains("comment-card") || template.contains("review-list"),
            "Expected product page template to render persisted comments."
        )
    }

    @Test
    fun `controller layer exposes product scoped comment submission and sorting`() {
        val controllerSources = kotlinSourcesUnder(controllersRoot)
        val productPageController = controllerSources.firstOrNull { it.name == "ProductPageController.kt" }
            ?: error("Expected ProductPageController.kt to exist.")

        val source = productPageController.readText()

        assertTrue(
            source.contains("@PostMapping") &&
                source.contains("/products/showcase") &&
                source.contains("name") &&
                source.contains("email") &&
                source.contains("comment") &&
                source.contains("rating"),
            "Expected ProductPageController to expose a POST endpoint for product comments with name, email, comment, and rating."
        )

        assertTrue(
            source.contains("sort") || source.contains("direction"),
            "Expected ProductPageController to accept a sort or direction parameter for rating order."
        )

        assertTrue(
            source.contains("variantId") || source.contains("variantInternalId"),
            "Expected ProductPageController to accept an optional selected variant reference when submitting a comment."
        )
    }

    @Test
    fun `repository layer persists product comments and supports sorted retrieval`() {
        val repositorySources = keywordMatchedSources(
            repositoriesRoot,
            listOf("comment", "review", "rating")
        )

        assertFalse(
            repositorySources.isEmpty(),
            "Expected a dedicated repository for product comments or ratings."
        )

        val repositorySource = repositorySources.joinToString("\n") { it.readText() }

        assertTrue(
            repositorySource.contains("product_internal_id") || repositorySource.contains("productInternalId"),
            "Expected the comment repository to scope persistence to a product."
        )
        assertTrue(
            repositorySource.contains("save(") || repositorySource.contains("create(") || repositorySource.contains("insert"),
            "Expected the comment repository to persist submitted comments."
        )
        assertTrue(
            repositorySource.contains("ORDER BY") || repositorySource.contains("sort") || repositorySource.contains("rating DESC"),
            "Expected the comment repository to support rating-based sorting."
        )
    }

    @Test
    fun `service layer orchestrates comment submission and sorted listing`() {
        val serviceSources = keywordMatchedSources(
            servicesRoot,
            listOf("comment", "review", "rating")
        )

        assertFalse(
            serviceSources.isEmpty(),
            "Expected a dedicated service for product comments or ratings."
        )

        val serviceSource = serviceSources.joinToString("\n") { it.readText() }

        assertTrue(
            serviceSource.contains("save(") || serviceSource.contains("create") || serviceSource.contains("submit"),
            "Expected the comment service to handle comment submission."
        )
        assertTrue(
            serviceSource.contains("sort") || serviceSource.contains("direction") || serviceSource.contains("DESC"),
            "Expected the comment service to handle rating sort direction."
        )
        assertTrue(
            serviceSource.contains("productInternalId") || serviceSource.contains("productId"),
            "Expected the comment service to work against a product identifier."
        )
    }

    @Test
    fun `http cases exist when a dedicated comment api controller is added`() {
        val apiControllerSources = keywordMatchedSources(
            controllersRoot.resolve("api"),
            listOf("comment", "review", "rating")
        ).filter { path ->
            val source = path.readText().lowercase()
            source.contains("@requestmapping(\"/api/") &&
                (source.contains("/comment") || source.contains("/review") || source.contains("/rating"))
        }

        if (apiControllerSources.isEmpty()) {
            return
        }

        val httpFiles = Files.list(httpTestsRoot)
            .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".http") }
            .toList()

        assertTrue(
            httpFiles.any { file ->
                val source = file.readText()
                source.contains("/api/") &&
                    (source.contains("comment", ignoreCase = true) ||
                        source.contains("review", ignoreCase = true) ||
                        source.contains("rating", ignoreCase = true))
            },
            "Expected HTTP test coverage for any dedicated comment-related API controller."
        )
    }

    private fun keywordMatchedSources(root: Path, keywords: List<String>): List<Path> {
        return kotlinSourcesUnder(root).filter { path ->
            val source = path.readText().lowercase()
            keywords.any(source::contains)
        }
    }

    private fun kotlinSourcesUnder(root: Path): List<Path> {
        if (!root.exists() || !root.isDirectory()) {
            return emptyList()
        }

        Files.walk(root).use { stream ->
            return stream
                .filter { Files.isRegularFile(it) && it.extension == "kt" }
                .toList()
        }
    }

    private fun assertContainsAll(source: String, values: List<String>, message: String) {
        val missing = values.filterNot(source::contains)
        assertTrue(missing.isEmpty(), "$message Missing: $missing")
    }
}
