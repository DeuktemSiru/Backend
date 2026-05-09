package com.deuktemsiru.service

import com.deuktemsiru.dto.BuyerStoreResponse
import com.deuktemsiru.dto.StoreResponse
import com.deuktemsiru.dto.UpdateStoreRequest
import com.deuktemsiru.entity.CategoryType
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Wishlist
import com.deuktemsiru.repository.MenuItemRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.StoreRepository
import com.deuktemsiru.repository.WishlistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class StoreService(
    private val storeRepository: StoreRepository,
    private val menuItemRepository: MenuItemRepository,
    private val wishlistRepository: WishlistRepository,
    private val productRepository: ProductRepository,
    private val memberService: MemberService,
) {

    fun getStores(userId: Long?): List<StoreResponse> {
        val stores = storeRepository.findAll()
        val wishlistedIds = userId?.let {
            val member = memberService.findMember(it)
            wishlistRepository.findByMember(member).map { w -> w.store.storeId }.toSet()
        } ?: emptySet()
        return stores.map { StoreResponse.from(it, it.storeId in wishlistedIds) }
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

    fun getWishlist(userId: Long): List<StoreResponse> {
        val member = memberService.findMember(userId)
        return wishlistRepository.findByMember(member).map { StoreResponse.from(it.store, isWishlisted = true) }
    }

    fun findStore(storeId: Long) =
        storeRepository.findById(storeId).orElseThrow { NoSuchElementException("가게를 찾을 수 없습니다.") }

    // ── 구매자 앱 전용 ─────────────────────────────────────────

    fun getStoresBuyer(category: String?, memberId: Long): List<BuyerStoreResponse> {
        val categoryType = category?.let { runCatching { CategoryType.valueOf(it) }.getOrNull() }
        val stores = if (categoryType != null) storeRepository.findByCategory(categoryType) else storeRepository.findAll()
        val today = LocalDate.now()
        val member = memberService.findMember(memberId)
        val wishlistedIds = wishlistRepository.findByMember(member).map { it.store.storeId }.toSet()
        return stores.map { store ->
            val products = productRepository.findByStoreAndAvailableDateAndStatus(store, today, ProductStatus.AVAILABLE)
            BuyerStoreResponse.from(store, products, store.storeId in wishlistedIds)
        }
    }

    fun getStoreBuyer(storeId: Long, memberId: Long): BuyerStoreResponse {
        val store = findStore(storeId)
        val today = LocalDate.now()
        val member = memberService.findMember(memberId)
        val isWishlisted = wishlistRepository.existsByMemberAndStore(member, store)
        val products = productRepository.findByStoreAndAvailableDateAndStatus(store, today, ProductStatus.AVAILABLE)
        return BuyerStoreResponse.from(store, products, isWishlisted)
    }

    fun getWishlistBuyer(memberId: Long): List<BuyerStoreResponse> {
        val member = memberService.findMember(memberId)
        val today = LocalDate.now()
        return wishlistRepository.findByMember(member).map { wishlist ->
            val store = wishlist.store
            val products = productRepository.findByStoreAndAvailableDateAndStatus(store, today, ProductStatus.AVAILABLE)
            BuyerStoreResponse.from(store, products, true)
        }
    }
}
