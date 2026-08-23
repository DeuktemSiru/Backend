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
    private val allowedExtensions = setOf("jpg", "jpeg", "png", "gif", "webp")

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
        require(extension in allowedExtensions) { "지원하지 않는 이미지 형식입니다." }
        require(hasAllowedImageSignature(image)) { "이미지 파일 형식이 올바르지 않습니다." }

        val filename = "${UUID.randomUUID()}.$extension"
        val target = root.resolve(filename).normalize()
        require(target.startsWith(root)) { "잘못된 파일 경로입니다." }

        image.inputStream.use { Files.copy(it, target) }
        return "/uploads/menu-images/$filename"
    }

    private fun hasAllowedImageSignature(image: MultipartFile): Boolean {
        val header = image.inputStream.use { it.readNBytes(12) }
        fun matches(offset: Int, vararg magic: Int) =
            header.size >= offset + magic.size &&
                magic.withIndex().all { (i, byte) -> header[offset + i] == byte.toByte() }

        return matches(0, 0xFF, 0xD8, 0xFF) ||                                       // JPEG
            matches(0, 0x89, 0x50, 0x4E, 0x47) ||                                    // PNG
            matches(0, 0x47, 0x49, 0x46) ||                                          // GIF
            (matches(0, 0x52, 0x49, 0x46, 0x46) && matches(8, 0x57, 0x45, 0x42, 0x50)) // WEBP
    }
}
