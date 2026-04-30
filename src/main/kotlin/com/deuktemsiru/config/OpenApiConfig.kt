package com.deuktemsiru.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val bearerAuth = "bearerAuth"

        return OpenAPI()
            .info(
                Info()
                    .title("Deuktemsiru API")
                    .description("Deuktemsiru backend API documentation")
                    .version("v1"),
            )
            .servers(listOf(Server().url("http://localhost:8080").description("Local server")))
            .components(
                Components().addSecuritySchemes(
                    bearerAuth,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )
            .addSecurityItem(SecurityRequirement().addList(bearerAuth))
    }
}
