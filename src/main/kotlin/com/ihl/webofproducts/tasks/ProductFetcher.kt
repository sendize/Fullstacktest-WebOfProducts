package com.ihl.webofproducts.tasks


import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.util.UriComponentsBuilder
import com.ihl.webofproducts.database.model.Product
import com.ihl.webofproducts.database.model.ProductVariant
import com.ihl.webofproducts.database.repository.ProductRepository
import tools.jackson.databind.ObjectMapper

@Component
class ProductFetcher(
    private val productRepository: ProductRepository
) {
    private val restTemplate = RestTemplate()
    private val baseUrl = "https://famme.no/products.json"
    private val limit = 50

    @Scheduled(initialDelay = 0)
    fun fetchJsonFromSite(): List<Product> {
        var products = emptyList<Product>()

        try {
            val noOfProductsInDb = productRepository.getCount()
            if (noOfProductsInDb >= limit) {
                println("DB already has more than 50 products. Not fetching from Famme.")
                return products
            }

            println("Fetching data from $baseUrl...")
            val uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("limit", limit)
                .build()
                .toUri()

            val resultString = restTemplate.getForObject<String>(uri) ?: ""
            products = parseProducts(resultString)
        } catch (ex: Exception) {
            println(ex.message)
            println(ex.stackTraceToString())
        }
        productRepository.batchInsert(products)
        println("Fetched ${products.size} products from Famme and saved to the database.")
        return products
    }

    fun parseProducts(productString: String): List<Product> {
        val mapper = ObjectMapper()
        val root = mapper.readTree(productString)
        val productsNode = root.get("products")

        val products = mutableListOf<Product>()
        for (productNode in productsNode) {
            val externalId = productNode.get("id").asLong()
            val bodyHTML = productNode.get("body_html").asText()
            val title = productNode.get("title").asText()
            val productType = productNode.get("product_type").asText()
            val images = productNode.get("images").take(1).map { it.get("src").asText() }

            val variantNode = productNode.get("variants")
            val variants = mutableListOf<ProductVariant>()
            for (variantNode in variantNode) {
                val variantExternalId = variantNode.get("id").asLong()
                val variantSKU = variantNode.get("sku").asText()
                val variantTitle = variantNode.get("title").asText()
                val variantPrice = variantNode.get("price").asDouble()
                val variantColor = variantNode.get("option1").asText()
                val variantSize = variantNode.get("option2").asText()
                val variantImageSrc = variantNode.get("featured_image")?.get("src")?.asText()

                variants.add(
                    ProductVariant(
                        externalId = variantExternalId,
                        sku = variantSKU,
                        title = variantTitle,
                        price = variantPrice,
                        color = variantColor,
                        size = variantSize,
                        imageUrl = variantImageSrc ?: "",
                    )
                )
//                println("Adding variant: $variantTitle with image: $variantImageSrc")
            }


//            println("Adding product: $title")
            products.add(
                Product(
                    externalId = externalId,
                    title = title,
                    bodyHTML = bodyHTML,
                    productType = productType,
                    imageUrl = images.first(),
                    variants = variants
                )
            )

        }
        return products
    }
}

//@Component
//fun main(){
//    ProductFetcher().fetchJson();
//}