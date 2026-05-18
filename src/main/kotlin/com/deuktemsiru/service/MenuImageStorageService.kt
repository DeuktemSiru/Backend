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
        val header = image.inputStream.use { input -> input.readNBytes(12) }
        if (header.size < 4) return false
        val isJpeg = header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()
        val isPng = header.copyOfRange(0, 4).contentEquals(byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte()))
        val isGif = header.copyOfRange(0, 3).contentEquals(byteArrayOf(0x47.toByte(), 0x49.toByte(), 0x46.toByte()))
        val isWebp = header.size >= 12 &&
            header.copyOfRange(0, 4).contentEquals(byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte())) &&
            header.copyOfRange(8, 12).contentEquals(byteArrayOf(0x57.toByte(), 0x45.toByte(), 0x42.toByte(), 0x50.toByte()))
        return isJpeg || isPng || isGif || isWebp
    }
}
