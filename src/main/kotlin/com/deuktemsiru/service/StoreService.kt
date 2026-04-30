package com.deuktemsiru.service

import com.deuktemsiru.dto.MenuItemRequest
import com.deuktemsiru.dto.MenuItemResponse
import com.deuktemsiru.dto.MenuItemUpdateRequest
import com.deuktemsiru.dto.StoreResponse
import com.deuktemsiru.dto.UpdateStoreRequest
import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.StoreCategory
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
    private val userService: UserService,
) {

    fun getStores(category: String?, userId: Long?): List<StoreResponse> {
        val stores = if (category != null) {
            storeRepository.findByCategory(StoreCategory.valueOf(category.uppercase()))
        } else {
            storeRepository.findAll()
        }
        val wishlistedIds = userId?.let {
            val user = userService.findUser(it)
            wishlistRepository.findByUser(user).map { w -> w.store.id }.toSet()
        } ?: emptySet()

        return stores.map { StoreResponse.from(it, it.id in wishlistedIds) }
    }

    fun getStore(storeId: Long, userId: Long?): StoreResponse {
        val store = findStore(storeId)
        val isWishlisted = userId?.let {
            val user = userService.findUser(it)
            wishlistRepository.existsByUserAndStore(user, store)
        } ?: false
        return StoreResponse.from(store, isWishlisted)
    }

    @Transactional
    fun updateStore(sellerId: Long, req: UpdateStoreRequest): StoreResponse {
        val seller = userService.findUser(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        req.address?.let { store.address = it }
        req.phone?.let { store.phone = it }
        req.closingTime?.let { store.closingTime = it }
        return StoreResponse.from(store)
    }

    fun getSellerStore(sellerId: Long): StoreResponse {
        val seller = userService.findUser(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        return StoreResponse.from(store)
    }

    @Transactional
    fun addMenuItem(sellerId: Long, req: MenuItemRequest): MenuItemResponse {
        require(req.originalPrice > 0) { "정상가는 0보다 커야 합니다." }
        require(req.discountRate in 1..99) { "할인율은 1~99 사이여야 합니다." }
        require(req.quantity > 0) { "수량은 0보다 커야 합니다." }

        val seller = userService.findUser(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }

        val discountedPrice = req.originalPrice * (100 - req.discountRate) / 100
        val item = MenuItem(
            store = store,
            name = req.name,
            emoji = req.emoji,
            imageUrl = req.imageUrl,
            originalPrice = req.originalPrice,
            discountedPrice = discountedPrice,
            discountRate = req.discountRate,
            remainingItems = req.quantity,
            pickupTimeSlot = req.pickupTimeSlot,
        )
        store.menuItems.add(item)
        return MenuItemResponse.from(menuItemRepository.save(item))
    }

    @Transactional
    fun updateMenuItem(sellerId: Long, menuItemId: Long, req: MenuItemUpdateRequest): MenuItemResponse {
        val seller = userService.findUser(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val item = menuItemRepository.findById(menuItemId)
            .orElseThrow { NoSuchElementException("메뉴를 찾을 수 없습니다.") }
        require(item.store.id == store.id) { "접근 권한이 없습니다." }

        req.remainingItems?.let {
            require(it >= 0) { "수량은 0 이상이어야 합니다." }
            item.remainingItems = it
            item.isSoldOut = it == 0
        }
        req.isSoldOut?.let { item.isSoldOut = it }
        req.pickupTimeSlot?.let { item.pickupTimeSlot = it }
        req.imageUrl?.let { item.imageUrl = it }
        req.discountRate?.let {
            require(it in 1..99) { "할인율은 1~99 사이여야 합니다." }
            item.discountRate = it
            item.discountedPrice = item.originalPrice * (100 - it) / 100
        }
        return MenuItemResponse.from(item)
    }

    @Transactional
    fun deleteMenuItem(sellerId: Long, menuItemId: Long) {
        val seller = userService.findUser(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        val item = menuItemRepository.findById(menuItemId)
            .orElseThrow { NoSuchElementException("메뉴를 찾을 수 없습니다.") }
        require(item.store.id == store.id) { "접근 권한이 없습니다." }
        store.menuItems.remove(item)
    }

    @Transactional
    fun toggleWishlist(userId: Long, storeId: Long): Boolean {
        val user = userService.findUser(userId)
        val store = findStore(storeId)
        val existing = wishlistRepository.findByUserAndStore(user, store)
        return if (existing.isPresent) {
            wishlistRepository.delete(existing.get())
            false
        } else {
            wishlistRepository.save(com.deuktemsiru.entity.Wishlist(user = user, store = store))
            true
        }
    }

    fun getWishlist(userId: Long): List<StoreResponse> {
        val user = userService.findUser(userId)
        return wishlistRepository.findByUser(user).map { StoreResponse.from(it.store, isWishlisted = true) }
    }

    fun findStore(storeId: Long) =
        storeRepository.findById(storeId).orElseThrow { NoSuchElementException("가게를 찾을 수 없습니다.") }
}
