package com.deuktemsiru.dto

import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Store
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "메뉴 응답")
data class MenuItemResponse(
    @field:Schema(description = "메뉴 ID", example = "1")
    val menuItemId: Long,
    @field:Schema(description = "메뉴명", example = "크루아상")
    val name: String,
    @field:Schema(description = "메뉴 설명", example = "버터 풍미가 진한 크루아상", nullable = true)
    val description: String?,
    @field:Schema(description = "메뉴 이미지 URL", example = "/uploads/menus/menu.png", nullable = true)
    val imageUrl: String?,
    @field:Schema(description = "정상가", example = "5000")
    val originalPrice: Int,
    @field:Schema(description = "알레르기 정보", example = "밀, 우유", nullable = true)
    val allergenInfo: String?,
    @field:Schema(description = "활성 여부", example = "true")
    val isActive: Boolean,
) {
    companion object {
        fun from(item: MenuItem) = MenuItemResponse(
            menuItemId = item.menuItemId,
            name = item.name,
            description = item.description,
            imageUrl = item.imageUrl,
            originalPrice = item.originalPrice,
            allergenInfo = item.allergenInfo,
            isActive = item.isActive,
        )
    }
}

@Schema(description = "가게 응답")
data class StoreResponse(
    @field:Schema(description = "가게 ID", example = "1")
    val storeId: Long,
    @field:Schema(description = "가게명", example = "시루 베이커리")
    val name: String,
    @field:Schema(description = "가게 설명", example = "당일 생산 베이커리를 판매합니다.", nullable = true)
    val description: String?,
    @field:Schema(description = "주소", example = "서울시 강남구 테헤란로 1")
    val address: String,
    @field:Schema(description = "위도", example = "37.5665")
    val latitude: Double,
    @field:Schema(description = "경도", example = "126.9780")
    val longitude: Double,
    @field:Schema(description = "전화번호", example = "02-1234-5678", nullable = true)
    val phone: String?,
    @field:Schema(description = "대표 이미지 URL", example = "/uploads/stores/store.png", nullable = true)
    val thumbnailUrl: String?,
    @field:Schema(description = "평균 평점", example = "4.7")
    val ratingAvg: Double,
    @field:Schema(description = "리뷰 수", example = "12")
    val reviewCount: Int,
    @field:Schema(description = "영업 활성 여부", example = "true")
    val isActive: Boolean,
    @field:Schema(description = "찜 여부", example = "true")
    val isWishlisted: Boolean,
    @field:Schema(description = "카테고리 목록", example = "[\"BAKERY\"]")
    val categories: List<String>,
    @field:Schema(description = "메뉴 목록")
    val menuItems: List<MenuItemResponse>,
) {
    companion object {
        fun from(store: Store, isWishlisted: Boolean = false) = StoreResponse(
            storeId = store.storeId,
            name = store.name,
            description = store.description,
            address = store.address,
            latitude = store.latitude,
            longitude = store.longitude,
            phone = store.phone,
            thumbnailUrl = store.thumbnailUrl,
            ratingAvg = store.ratingAvg,
            reviewCount = store.reviewCount,
            isActive = store.isActive,
            isWishlisted = isWishlisted,
            categories = store.categories.map { it.category.name },
            menuItems = store.menuItems.map { MenuItemResponse.from(it) },
        )
    }
}

@Schema(description = "가게 수정 요청")
data class UpdateStoreRequest(
    @field:Schema(description = "가게 설명", example = "당일 생산 베이커리를 판매합니다.", nullable = true)
    val description: String? = null,
    @field:Schema(description = "전화번호", example = "02-1234-5678", nullable = true)
    val phone: String? = null,
)
