package com.deuktemsiru.service

import com.deuktemsiru.dto.CreateStoreRequest
import com.deuktemsiru.dto.ProductDetailResponse
import com.deuktemsiru.dto.ProductListItemResponse
import com.deuktemsiru.dto.StoreDetailResponse
import com.deuktemsiru.dto.StoreListItemResponse
import com.deuktemsiru.dto.StoreMarkerResponse
import com.deuktemsiru.dto.StoreResponse
import com.deuktemsiru.dto.UpdateStoreRequest
import com.deuktemsiru.dto.WishlistItemResponse
import com.deuktemsiru.entity.CategoryType
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import com.deuktemsiru.entity.StoreCategory
import com.deuktemsiru.entity.Wishlist
import com.deuktemsiru.repository.MenuItemRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.StoreCategoryRepository
import com.deuktemsiru.repository.StoreRepository
import com.deuktemsiru.repository.WishlistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Service
@Transactional(readOnly = true)
class StoreService(
    private val storeRepository: StoreRepository,
    private val menuItemRepository: MenuItemRepository,
    private val wishlistRepository: WishlistRepository,
    private val productRepository: ProductRepository,
    private val storeCategoryRepository: StoreCategoryRepository,
    private val memberService: MemberService,
) {
    data class PageSlice<T>(val items: List<T>, val hasNext: Boolean)

    // ── 기존 판매자용 ──────────────────────────────────────────────────────────

    fun getStores(userId: Long?): List<Store> {
        return storeRepository.findAll()
    }

    fun getStore(storeId: Long, userId: Long?): StoreResponse {
        val store = findStore(storeId)
        val isWishlisted = userId?.let {
            val member = memberService.findMember(it)
            wishlistRepository.existsByMemberAndStore(member, store)
        } ?: false
        return StoreResponse.from(store, isWishlisted)
    }

    @Transactional
    fun updateStore(sellerId: Long, req: UpdateStoreRequest): StoreResponse {
        val seller = memberService.findMember(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        req.description?.let { store.description = it }
        req.phone?.let { store.phone = it }
        return StoreResponse.from(store)
    }

    fun getSellerStore(sellerId: Long): StoreResponse {
        val seller = memberService.findMember(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        return StoreResponse.from(store)
    }

    @Transactional
    fun toggleWishlist(userId: Long, storeId: Long): Boolean {
        val member = memberService.findMember(userId)
        val store = findStore(storeId)
        val existing = wishlistRepository.findByMemberAndStore(member, store)
        return if (existing.isPresent) {
            wishlistRepository.delete(existing.get())
            false
        } else {
            wishlistRepository.save(Wishlist(member = member, store = store))
            true
        }
    }

    @Transactional
    fun addWishlist(userId: Long, storeId: Long) {
        val member = memberService.findMember(userId)
        val store = findStore(storeId)
        val existing = wishlistRepository.findByMemberAndStore(member, store)
        if (existing.isPresent) throw IllegalStateException("이미 찜한 가게입니다.")
        wishlistRepository.save(Wishlist(member = member, store = store))
    }

    @Transactional
    fun removeWishlist(userId: Long, storeId: Long) {
        val member = memberService.findMember(userId)
        val store = findStore(storeId)
        val existing = wishlistRepository.findByMemberAndStore(member, store)
            .orElseThrow { NoSuchElementException("찜한 가게가 아닙니다.") }
        wishlistRepository.delete(existing)
    }

    fun findStore(storeId: Long) =
        storeRepository.findById(storeId).orElseThrow { NoSuchElementException("가게를 찾을 수 없습니다.") }

    // ── 구매자 앱 가게 목록 (GET /stores) ────────────────────────────────────

    fun getStoreListBuyer(
        category: String?,
        keyword: String?,
        memberId: Long?,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Int? = null,
        sort: String = "distance",
        page: Int = 0,
        size: Int = 20,
    ): PageSlice<StoreListItemResponse> {
        val categoryType = category?.let { runCatching { CategoryType.valueOf(it) }.getOrNull() }
        val allStores = if (categoryType != null)
            storeRepository.findByCategory(categoryType)
        else
            storeRepository.findAll()

        val today = LocalDate.now()
        val filtered = if (keyword != null)
            allStores.filter { it.name.contains(keyword, ignoreCase = true) }
        else
            allStores

        val withDistance = filtered.map { store -> store to distanceMeters(latitude, longitude, store.latitude, store.longitude) }
            .filter { (_, distance) -> radius == null || distance <= radius }
        val sorted = when (sort.lowercase()) {
            "rating" -> withDistance.sortedWith(compareByDescending<Pair<Store, Int>> { it.first.ratingAvg }.thenBy { it.second })
            "products", "available" -> withDistance.sortedByDescending {
                productRepository.findByStoreAndAvailableDateAndStatus(it.first, today, ProductStatus.AVAILABLE).size
            }
            else -> withDistance.sortedBy { it.second }
        }
        return sorted.map { (store, distance) ->
            val count = productRepository.findByStoreAndAvailableDateAndStatus(store, today, ProductStatus.AVAILABLE).size
            StoreListItemResponse.from(store, availableProductCount = count, distanceM = distance)
        }.pageSlice(page, size)
    }

    // ── 구매자 앱 가게 상세 (GET /stores/{storeId}) ───────────────────────────

    fun getStoreDetailBuyer(storeId: Long): StoreDetailResponse {
        val store = findStore(storeId)
        val today = LocalDate.now()
        val products = productRepository.findByStoreAndAvailableDateAndStatus(store, today, ProductStatus.AVAILABLE)
        return StoreDetailResponse.from(store, products)
    }

    // ── 지도 마커 (GET /stores/map) ───────────────────────────────────────────

    fun getMapMarkers(category: String?): List<StoreMarkerResponse> {
        val categoryType = category?.let { runCatching { CategoryType.valueOf(it) }.getOrNull() }
        val stores = if (categoryType != null)
            storeRepository.findByCategory(categoryType)
        else
            storeRepository.findAll()
        val today = LocalDate.now()
        return stores.map { store ->
            val count = productRepository.findByStoreAndAvailableDateAndStatus(store, today, ProductStatus.AVAILABLE).size
            StoreMarkerResponse.from(store, availableProductCount = count)
        }
    }

    // ── 상품 목록 (GET /products) ─────────────────────────────────────────────

    fun getProductsBuyer(
        category: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Int? = null,
        sort: String = "distance",
        page: Int = 0,
        size: Int = 20,
    ): PageSlice<ProductListItemResponse> {
        val categoryType = category?.let { runCatching { CategoryType.valueOf(it) }.getOrNull() }
        val stores = if (categoryType != null)
            storeRepository.findByCategory(categoryType)
        else
            storeRepository.findAll()
        val today = LocalDate.now()
        val products = stores.flatMap { store ->
            val distance = distanceMeters(latitude, longitude, store.latitude, store.longitude)
            if (radius != null && distance > radius) return@flatMap emptyList()
            productRepository.findByStoreAndAvailableDateAndStatus(store, today, ProductStatus.AVAILABLE)
                .map { ProductListItemResponse.from(it, distanceM = distance) }
        }
        val sorted = when (sort.lowercase()) {
            "discount" -> products.sortedBy { it.discountPrice }
            "quantity" -> products.sortedByDescending { it.quantityRemaining }
            else -> products.sortedBy { it.distanceM }
        }
        return sorted.pageSlice(page, size)
    }

    // ── 상품 상세 (GET /products/{productId}) ────────────────────────────────

    fun getProductDetailBuyer(productId: Long): ProductDetailResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("상품을 찾을 수 없습니다.") }
        return ProductDetailResponse.from(product)
    }

    // ── 찜 목록 (GET /wishlist) ───────────────────────────────────────────────

    fun getWishlistBuyer(memberId: Long): List<WishlistItemResponse> {
        val member = memberService.findMember(memberId)
        val today = LocalDate.now()
        return wishlistRepository.findByMember(member).map { w ->
            val store = w.store
            val count = productRepository.findByStoreAndAvailableDateAndStatus(store, today, ProductStatus.AVAILABLE).size
            WishlistItemResponse(
                wishlistId = w.wishlistId,
                storeId = store.storeId,
                name = store.name,
                thumbnailUrl = store.thumbnailUrl,
                ratingAvg = store.ratingAvg,
                availableProductCount = count,
            )
        }
    }

    // ── 가게 등록 (POST /sellers/stores) ─────────────────────────────────────

    @Transactional
    fun createStore(sellerId: Long, req: CreateStoreRequest, thumbnailUrl: String? = null): Store {
        val seller = memberService.findMember(sellerId)
        val store = storeRepository.save(
            Store(
                owner = seller,
                name = req.name,
                description = req.description,
                address = req.address,
                latitude = req.latitude,
                longitude = req.longitude,
                phone = req.phone,
                thumbnailUrl = thumbnailUrl,
            )
        )
        req.categories.forEach { cat ->
            val categoryType = runCatching { CategoryType.valueOf(cat) }
                .getOrElse { throw IllegalArgumentException("지원하지 않는 카테고리: $cat") }
            storeCategoryRepository.save(StoreCategory(store = store, category = categoryType))
        }
        return store
    }

    private fun distanceMeters(
        fromLatitude: Double?,
        fromLongitude: Double?,
        toLatitude: Double,
        toLongitude: Double,
    ): Int {
        if (fromLatitude == null || fromLongitude == null) return 0
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(toLatitude - fromLatitude)
        val dLon = Math.toRadians(toLongitude - fromLongitude)
        val lat1 = Math.toRadians(fromLatitude)
        val lat2 = Math.toRadians(toLatitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadiusMeters * c).roundToInt()
    }

    private fun <T> List<T>.pageSlice(page: Int, size: Int): PageSlice<T> {
        val safeSize = size.coerceAtLeast(1)
        val fromIndex = page.coerceAtLeast(0) * safeSize
        if (fromIndex >= this.size) return PageSlice(emptyList(), false)
        val toIndex = minOf(fromIndex + safeSize, this.size)
        return PageSlice(subList(fromIndex, toIndex), toIndex < this.size)
    }
}
