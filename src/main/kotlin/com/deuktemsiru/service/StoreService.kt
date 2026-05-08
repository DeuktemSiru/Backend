package com.deuktemsiru.service

import com.deuktemsiru.dto.StoreResponse
import com.deuktemsiru.dto.UpdateStoreRequest
import com.deuktemsiru.entity.Wishlist
import com.deuktemsiru.repository.MenuItemRepository
import com.deuktemsiru.repository.StoreRepository
import com.deuktemsiru.repository.WishlistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StoreService(
    private val storeRepository: StoreRepository,
    private val menuItemRepository: MenuItemRepository,
    private val wishlistRepository: WishlistRepository,
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
}
