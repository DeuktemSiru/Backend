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

        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("상품을 찾을 수 없습니다.") }
        require(product.status == ProductStatus.AVAILABLE) { "${product.name}은(는) 구매 불가 상태입니다." }

        val existingItems = cartItemRepository.findByMemberOrderByCreatedAtAsc(member)
        val existing = cartItemRepository.findByMemberAndProduct(member, product)
        val nextQuantity = existing.map { it.quantity + quantity }.orElse(quantity)
        require(product.quantityRemaining >= nextQuantity) { "${product.name} 재고가 부족합니다." }

        val otherStoreItem = existingItems.firstOrNull { it.product.store.storeId != product.store.storeId }
        require(otherStoreItem == null) { "장바구니에는 같은 가게의 상품만 담을 수 있습니다." }

        val saved = if (existing.isPresent) {
            existing.get().apply { this.quantity = nextQuantity }
        } else {
            cartItemRepository.save(CartItemEntity(member = member, product = product, quantity = quantity))
        }
        return toResponse(saved)
    }

    fun getCart(memberId: Long): CartResponse {
        val member = memberService.findMember(memberId)
        val items = cartItemRepository.findByMemberOrderByCreatedAtAsc(member).map { toResponse(it) }
        return CartResponse(
            items = items,
            totalPrice = items.sumOf { it.discountPrice * it.quantity },
        )
    }

    @Transactional
    fun removeFromCart(memberId: Long, cartItemId: Long) {
        val member = memberService.findMember(memberId)
        val cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow { NoSuchElementException("장바구니 상품을 찾을 수 없습니다.") }
        require(cartItem.member.memberId == member.memberId) { "접근 권한이 없습니다." }
        cartItemRepository.delete(cartItem)
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
            discountPrice = product.discountPrice,
            quantity = cartItem.quantity,
            imageUrl = product.thumbnailUrl,
        )
    }
}
