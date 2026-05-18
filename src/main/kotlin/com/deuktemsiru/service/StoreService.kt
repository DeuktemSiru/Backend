package com.deuktemsiru.service

import com.deuktemsiru.common.toEnumOrNull
import com.deuktemsiru.common.toEnumOrThrow
import com.deuktemsiru.common.orNotFound
import com.deuktemsiru.common.today
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
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import com.deuktemsiru.entity.StoreCategory
import com.deuktemsiru.entity.StoreImage
import com.deuktemsiru.entity.Wishlist
import com.deuktemsiru.entity.requirePurchasableOn
import com.deuktemsiru.repository.MenuItemRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.StoreCategoryRepository
import com.deuktemsiru.repository.StoreRepository
import com.deuktemsiru.repository.WishlistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
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
    private val storeOwnershipService: StoreOwnershipService,
    private val clock: Clock,
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
        val store = storeOwnershipService.findSellerStore(sellerId)
        req.description?.let { store.description = it }
        req.phone?.let { store.phone = it }
        return StoreResponse.from(store)
    }

    fun getSellerStore(sellerId: Long): StoreResponse {
        return StoreResponse.from(storeOwnershipService.findSellerStore(sellerId))
    }

    @Transactional
    fun toggleWishlist(userId: Long, storeId: Long): Boolean {
        val (member, store, existing) = findWishlistEntry(userId, storeId)
        return if (existing != null) {
            wishlistRepository.delete(existing)
            false
        } else {
            wishlistRepository.save(Wishlist(member = member, store = store))
            true
        }
    }

    @Transactional
    fun addWishlist(userId: Long, storeId: Long) {
        val (member, store, existing) = findWishlistEntry(userId, storeId)
        if (existing != null) throw IllegalStateException("이미 찜한 가게입니다.")
        wishlistRepository.save(Wishlist(member = member, store = store))
    }

    @Transactional
    fun removeWishlist(userId: Long, storeId: Long) {
        val (_, _, existing) = findWishlistEntry(userId, storeId)
        wishlistRepository.delete(existing ?: throw NoSuchElementException("찜한 가게가 아닙니다."))
    }

    private fun findWishlistEntry(userId: Long, storeId: Long): Triple<com.deuktemsiru.entity.Member, Store, Wishlist?> {
        val member = memberService.findMember(userId)
        val store = findStore(storeId)
        return Triple(member, store, wishlistRepository.findByMemberAndStore(member, store).orElse(null))
    }

    fun findStore(storeId: Long) =
        storeRepository.findById(storeId).orNotFound("가게를 찾을 수 없습니다.")

    // ── 구매자 앱 가게 목록 (GET /stores) ────────────────────────────────────

    fun getStoreListBuyer(
        category: String?,
        keyword: String?,
        memberId: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Int? = null,
        sort: String = "distance",
        page: Int = 0,
        size: Int = 20,
    ): PageSlice<StoreListItemResponse> {
        val allStores = findStoresByCategory(category)

        val today = today()
        val productsByStore = displayProductsByStore(allStores, today)
        val availableCounts = productsByStore.mapValues { (_, products) -> products.sumOf { it.quantityRemaining } }
        val representativeProducts = productsByStore.mapValues { (_, products) -> representativeProduct(products) }
        val wishlistedStoreIds = wishlistedStoreIds(memberId, allStores)
        val hasLocation = hasLocation(latitude, longitude)
        val filtered = if (keyword != null)
            allStores.filter { it.name.contains(keyword, ignoreCase = true) }
        else
            allStores

        val withDistance = storesWithDistance(filtered, latitude, longitude, radius)
        val sorted = when (sort.lowercase()) {
            "rating" -> withDistance.sortedWith(compareByDescending<Pair<Store, Int>> { it.first.ratingAvg }.thenBy { it.second })
            "products", "available" -> withDistance.sortedByDescending {
                availableCounts[it.first.storeId] ?: 0
            }
            else -> if (hasLocation) withDistance.sortedBy { it.second } else withDistance.sortedBy { it.first.name }
        }
        return sorted.map { (store, distance) ->
            val count = availableCounts[store.storeId] ?: 0
            StoreListItemResponse.from(
                store,
                availableProductCount = count,
                distanceM = distance,
                representativeProduct = representativeProducts[store.storeId],
                isWishlisted = store.storeId in wishlistedStoreIds,
            )
        }.toPaged(page, size)
    }

    // ── 구매자 앱 가게 상세 (GET /stores/{storeId}) ───────────────────────────

    fun getStoreDetailBuyer(storeId: Long, memberId: Long? = null): StoreDetailResponse {
        val store = findStore(storeId)
        val today = today()
        val products = availableProducts(store, today)
        return StoreDetailResponse.from(store, products, isWishlisted(memberId, store))
    }

    private fun wishlistedStoreIds(memberId: Long?, stores: List<Store>): Set<Long> {
        val member = memberId?.let { memberService.findMember(it) } ?: return emptySet()
        if (stores.isEmpty()) return emptySet()
        return stores
            .filter { wishlistRepository.existsByMemberAndStore(member, it) }
            .mapTo(mutableSetOf()) { it.storeId }
    }

    private fun isWishlisted(memberId: Long?, store: Store): Boolean {
        val member = memberId?.let { memberService.findMember(it) } ?: return false
        return wishlistRepository.existsByMemberAndStore(member, store)
    }

    // ── 지도 마커 (GET /stores/map) ───────────────────────────────────────────

    fun getMapMarkers(category: String?): List<StoreMarkerResponse> {
        val stores = findStoresByCategory(category)
        val today = today()
        val counts = availableProductCounts(stores, today)
        return stores.map { store ->
            StoreMarkerResponse.from(store, availableProductCount = counts[store.storeId] ?: 0)
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
        val stores = findStoresByCategory(category)
        val today = today()
        val hasLocation = hasLocation(latitude, longitude)
        val nearbyStoreDistances = storesWithDistance(stores, latitude, longitude, radius)
        val nearbyStores = nearbyStoreDistances.map { it.first }
        val storeDistances = nearbyStoreDistances.associate { (store, distance) -> store.storeId to distance }
        val products = if (nearbyStores.isEmpty()) {
            emptyList()
        } else {
            productRepository.findByStoreInAndAvailableDateAndStatusAndQuantityRemainingGreaterThan(
                nearbyStores,
                today,
                ProductStatus.AVAILABLE,
                0,
            )
                .map { ProductListItemResponse.from(it, distanceM = storeDistances[it.store.storeId] ?: 0) }
        }
        val sorted = when (sort.lowercase()) {
            "discount" -> products.sortedBy { it.discountPrice }
            "quantity" -> products.sortedByDescending { it.quantityRemaining }
            else -> if (hasLocation) products.sortedBy { it.distanceM } else products.sortedBy { it.name }
        }
        return sorted.toPaged(page, size)
    }

    // ── 상품 상세 (GET /products/{productId}) ────────────────────────────────

    fun getProductDetailBuyer(productId: Long): ProductDetailResponse {
        val product = productRepository.findById(productId).orNotFound("상품을 찾을 수 없습니다.")
        product.requirePurchasableOn(today(), quantity = 1)
        return ProductDetailResponse.from(product)
    }

    // ── 찜 목록 (GET /wishlist) ───────────────────────────────────────────────

    fun getWishlistBuyer(memberId: Long): List<WishlistItemResponse> {
        val member = memberService.findMember(memberId)
        val today = today()
        val wishlists = wishlistRepository.findByMember(member)
        val stores = wishlists.map { it.store }
        val productsByStore = displayProductsByStore(stores, today)
        val counts = productsByStore.mapValues { (_, products) -> products.sumOf { it.quantityRemaining } }
        val representativeProducts = productsByStore.mapValues { (_, products) -> representativeProduct(products) }
        return wishlists.map { w ->
            val store = w.store
            val representativeProduct = representativeProducts[store.storeId]
            val originalPrice = representativeProduct?.originalPrice ?: 0
            val discountPrice = representativeProduct?.discountPrice ?: 0
            WishlistItemResponse(
                wishlistId = w.wishlistId,
                storeId = store.storeId,
                name = store.name,
                thumbnailUrl = store.thumbnailUrl,
                category = store.categories.firstOrNull()?.category?.name ?: "OTHER",
                ratingAvg = store.ratingAvg,
                availableProductCount = counts[store.storeId] ?: 0,
                representativeOriginalPrice = originalPrice,
                representativeDiscountPrice = discountPrice,
                representativeDiscountRate = if (originalPrice > 0) {
                    ((originalPrice - discountPrice) * 100 / originalPrice).coerceAtLeast(0)
                } else {
                    0
                },
                representativePickupEnd = representativeProduct?.pickupEnd?.toString(),
            )
        }
    }

    // ── 가게 등록 (POST /sellers/stores) ─────────────────────────────────────

    @Transactional
    fun createStore(sellerId: Long, req: CreateStoreRequest, thumbnailUrl: String? = null): Store {
        val seller = memberService.findMember(sellerId)
        require(seller.role == MemberRole.SELLER) { "판매자 계정만 가게를 등록할 수 있습니다." }
        require(!storeRepository.existsByOwner(seller)) { "이미 등록된 가게가 있습니다." }
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
            val categoryType = cat.toEnumOrThrow<CategoryType>("카테고리")
            storeCategoryRepository.save(StoreCategory(store = store, category = categoryType))
        }
        return store
    }

    @Transactional
    fun createStore(
        sellerId: Long,
        req: CreateStoreRequest,
        thumbnailUrl: String?,
        imageUrls: List<String>,
    ): Store {
        val store = createStore(sellerId, req, thumbnailUrl)
        imageUrls.forEachIndexed { index, imageUrl ->
            store.images += StoreImage(store = store, imageUrl = imageUrl, displayOrder = index)
        }
        return store
    }

    private fun availableProductCount(store: Store, date: LocalDate): Int =
        availableProductCounts(listOf(store), date)[store.storeId] ?: 0

    private fun today(): LocalDate = clock.today()

    private fun findStoresByCategory(category: String?): List<Store> =
        category?.toEnumOrNull<CategoryType>()?.let { storeRepository.findByCategory(it) }
            ?: storeRepository.findAll()

    private fun hasLocation(latitude: Double?, longitude: Double?) = latitude != null && longitude != null

    private fun storesWithDistance(
        stores: List<Store>,
        latitude: Double?,
        longitude: Double?,
        radius: Int?,
    ): List<Pair<Store, Int>> {
        val hasLocation = hasLocation(latitude, longitude)
        return stores
            .map { store -> store to distanceMeters(latitude, longitude, store.latitude, store.longitude) }
            .filter { (_, distance) -> !hasLocation || radius == null || distance <= radius }
    }

    private fun availableProductCounts(stores: List<Store>, date: LocalDate): Map<Long, Int> {
        val storeIds = stores.map { it.storeId }
        if (storeIds.isEmpty()) return emptyMap()
        return productRepository.countAvailableProductsByStoreIds(storeIds, date)
            .associate { it.storeId to it.productCount.toInt() }
    }

    private fun displayProductsByStore(stores: List<Store>, date: LocalDate): Map<Long, List<com.deuktemsiru.entity.Product>> {
        if (stores.isEmpty()) return emptyMap()
        val productsByStore = productRepository
            .findByStoreInAndAvailableDateAndStatusAndQuantityRemainingGreaterThan(stores, date, ProductStatus.AVAILABLE, 0)
            .groupBy { it.store.storeId }
        return stores.associate { store ->
            store.storeId to productsByStore[store.storeId].orEmpty()
        }
    }

    private fun availableProducts(store: Store, date: LocalDate): List<com.deuktemsiru.entity.Product> =
        productRepository.findByStoreAndAvailableDateAndStatusAndQuantityRemainingGreaterThan(
            store,
            date,
            ProductStatus.AVAILABLE,
            0,
        )

    private fun representativeProduct(products: List<com.deuktemsiru.entity.Product>): com.deuktemsiru.entity.Product? {
        return products.maxWithOrNull(
            compareBy<com.deuktemsiru.entity.Product> {
                if (it.originalPrice > 0) (it.originalPrice - it.discountPrice).toDouble() / it.originalPrice else 0.0
            }.thenByDescending { it.quantityRemaining }
        )
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

    /** S6: 리스트를 페이지 단위로 잘라 PageSlice 를 반환하는 확장 함수 */
    private fun <T> List<T>.toPaged(page: Int, size: Int): PageSlice<T> {
        val safeSize = size.coerceAtLeast(1)
        val fromIndex = page.coerceAtLeast(0) * safeSize
        if (fromIndex >= this.size) return PageSlice(emptyList(), false)
        val toIndex = minOf(fromIndex + safeSize, this.size)
        return PageSlice(subList(fromIndex, toIndex), toIndex < this.size)
    }
}
