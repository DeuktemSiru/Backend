package com.deuktemsiru.service

import com.deuktemsiru.controller.cart.CartItem
import com.deuktemsiru.controller.cart.CartResponse
import com.deuktemsiru.entity.CartItem as CartItemEntity
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.repository.CartItemRepository
import com.deuktemsiru.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CartService(
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val memberService: MemberService,
) {

    @Transactional
    fun addToCart(memberId: Long, productId: Long, quantity: Int): CartItem {
        require(quantity > 0) { "장바구니 수량은 1개 이상이어야 합니다." }
        val member = memberService.findMember(memberId)
        require(member.role == MemberRole.CONSUMER) { "소비자 계정만 장바구니를 사용할 수 있습니다." }

        // B3: 재고 확인을 pessimistic lock 으로 수행하여 overselling 방지
        val product = productRepository.findByIdForUpdate(productId)
            .orElseThrow { NoSuchElementException("상품을 찾을 수 없습니다.") }
        require(product.status == ProductStatus.AVAILABLE) { "${product.name}은(는) 구매 불가 상태입니다." }

        val existingItems = cartItemRepository.findByMemberOrderByCreatedAtAsc(member)
        val existing = existingItems.find { it.product.productId == product.productId }
        val nextQuantity = existing?.let { it.quantity + quantity } ?: quantity
        require(product.quantityRemaining >= nextQuantity) { "${product.name} 재고가 부족합니다." }

        val otherStoreItem = existingItems.firstOrNull { it.product.store.storeId != product.store.storeId }
        require(otherStoreItem == null) { "장바구니에는 같은 가게의 상품만 담을 수 있습니다." }

        val saved = if (existing != null) {
            existing.apply { this.quantity = nextQuantity }
        } else {
            cartItemRepository.save(CartItemEntity(member = member, product = product, quantity = quantity))
        }
        return toResponse(saved)
    }

    fun getCart(memberId: Long): CartResponse {
        // S5: Member 엔티티 로딩 없이 memberId 로 직접 조회
        val items = cartItemRepository.findByMemberMemberIdOrderByCreatedAtAsc(memberId).map { toResponse(it) }
        return CartResponse(
            items = items,
            totalPrice = items.sumOf { it.discountPrice * it.quantity },
        )
    }

    @Transactional
    fun removeFromCart(memberId: Long, cartItemId: Long) {
        // S5: 소유권 확인에 memberId 를 직접 사용
        val cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow { NoSuchElementException("장바구니 상품을 찾을 수 없습니다.") }
        require(cartItem.member.memberId == memberId) { "접근 권한이 없습니다." }
        cartItemRepository.delete(cartItem)
    }

    @Transactional
    fun updateQuantity(memberId: Long, cartItemId: Long, quantity: Int): CartItem {
        require(quantity > 0) { "장바구니 수량은 1개 이상이어야 합니다." }
        // S5: 소유권 확인에 memberId 를 직접 사용
        val cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow { NoSuchElementException("장바구니 상품을 찾을 수 없습니다.") }
        require(cartItem.member.memberId == memberId) { "접근 권한이 없습니다." }
        require(cartItem.product.status == ProductStatus.AVAILABLE) { "${cartItem.product.name}은(는) 구매 불가 상태입니다." }
        require(cartItem.product.quantityRemaining >= quantity) { "${cartItem.product.name} 재고가 부족합니다." }
        cartItem.quantity = quantity
        return toResponse(cartItem)
    }

    @Transactional
    fun clearCart(memberId: Long) {
        val member = memberService.findMember(memberId)
        cartItemRepository.deleteByMember(member)
    }

    private fun toResponse(cartItem: CartItemEntity): CartItem {
        val product = cartItem.product
        val store = product.store
        return CartItem(
            cartItemId = cartItem.cartItemId,
            productId = product.productId,
            productName = product.name,
            storeId = store.storeId,
            storeName = store.name,
            storeLatitude = store.latitude,
            storeLongitude = store.longitude,
            originalPrice = product.originalPrice,
            discountPrice = product.discountPrice,
            pickupStart = product.pickupStart.toString(),
            pickupEnd = product.pickupEnd.toString(),
            quantity = cartItem.quantity,
            imageUrl = product.thumbnailUrl,
        )
    }
}
