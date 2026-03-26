package com.ihl.webofproducts

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.hamcrest.Matchers.containsString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
    ]
)
class WebOfProductsApplicationTests {

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `index page renders with header footer and htmx wiring`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("<header class=\"site-header card\">")))
            .andExpect(content().string(containsString("<footer class=\"site-footer card\">")))
            .andExpect(content().string(containsString("hx-get=\"/products/load\"")))
            .andExpect(content().string(containsString("hx-post=\"/products/add\"")))
            .andExpect(content().string(containsString("kit.webawesome.com/1a257bfedcd54677.js")))
            .andExpect(content().string(containsString("htmx.org@2.0.8")))
            .andExpect(content().string(containsString("/css/tokens.css")))
            .andExpect(content().string(containsString("/css/index.css")))
            .andExpect(content().string(containsString("id=\"themeToggleBtn\"")))
            .andExpect(content().string(containsString("hx-post=\"/theme/toggle\"")))
            .andExpect(content().string(containsString("data-theme=\"light\"")))
    }

    @Test
    fun `load products endpoint returns starter product rows`() {
        mockMvc.perform(get("/products/load"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("<td>1</td>")))
            .andExpect(content().string(containsString("<td>Wireless Mouse</td>")))
            .andExpect(content().string(containsString("<td>Accessories</td>")))
            .andExpect(content().string(containsString("<td>$29.99</td>")))
    }

    @Test
    fun `add product endpoint appends a new row in session`() {
        val session = MockHttpSession()
        mockMvc.perform(get("/products/load").session(session))
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/products/add")
                .session(session)
                .param("name", "Laptop Stand")
                .param("category", "Accessories")
                .param("price", "49.90")
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("<td>5</td>")))
            .andExpect(content().string(containsString("<td>Laptop Stand</td>")))
            .andExpect(content().string(containsString("<td>$49.90</td>")))
    }

    @Test
    fun `theme toggle endpoint flips light and dark in session`() {
        val session = MockHttpSession()

        mockMvc.perform(post("/theme/toggle").session(session))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("data-theme=\"dark\"")))
            .andExpect(content().string(containsString("Switch to Light")))

        mockMvc.perform(post("/theme/toggle").session(session))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("data-theme=\"light\"")))
            .andExpect(content().string(containsString("Switch to Dark")))
    }

}
