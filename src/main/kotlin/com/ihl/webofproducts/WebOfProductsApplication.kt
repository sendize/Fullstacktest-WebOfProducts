package com.ihl.webofproducts

import io.github.cdimascio.dotenv.dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class WebOfProductsApplication

fun main(args: Array<String>) {
    val dotenv = dotenv {
        systemProperties = true
        ignoreIfMissing = true
    }
    runApplication<WebOfProductsApplication>(*args)
}
