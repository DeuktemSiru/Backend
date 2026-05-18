package com.deuktemsiru.config

import com.deuktemsiru.security.CurrentMemberIdArgumentResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Files
import java.nio.file.Paths

@Configuration
class WebConfig(
    @Value("\${app.upload.menu-image-dir:uploads/menu-images}")
    private val menuImageDir: String,
    private val currentMemberIdArgumentResolver: CurrentMemberIdArgumentResolver,
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val path = Paths.get(menuImageDir).toAbsolutePath().normalize()
        Files.createDirectories(path)
        val location = path.toUri().toString().let {
            if (it.endsWith("/")) it else "$it/"
        }
        registry.addResourceHandler("/uploads/menu-images/**")
            .addResourceLocations(location)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentMemberIdArgumentResolver)
    }
}
