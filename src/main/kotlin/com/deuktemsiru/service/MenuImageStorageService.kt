package com.deuktemsiru.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

@Service
class MenuImageStorageService(
    @Value("\${app.upload.menu-image-dir:uploads/menu-images}")
    private val uploadDir: String,
) {
    private val root: Path by lazy {
        Paths.get(uploadDir).toAbsolutePath().normalize().also { Files.createDirectories(it) }
    }

    fun save(image: MultipartFile?): String? {
        if (image == null || image.isEmpty) return null

        val contentType = image.contentType.orEmpty()
        require(contentType.startsWith("image/")) { "이미지 파일만 업로드할 수 있습니다." }

        val extension = image.originalFilename
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()
            ?: contentType.substringAfter("image/", "jpg")

        val filename = "${UUID.randomUUID()}.$extension"
        val target = root.resolve(filename).normalize()
        require(target.startsWith(root)) { "잘못된 파일 경로입니다." }

        image.inputStream.use { Files.copy(it, target) }
        return "/uploads/menu-images/$filename"
    }
}
