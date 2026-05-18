package com.deuktemsiru.service

import com.deuktemsiru.dto.CartItem
import com.deuktemsiru.dto.CartResponse
import com.deuktemsiru.entity.CartItem as CartItemEntity
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.requirePurchasableOn
import com.deuktemsiru.common.orNotFound
import com.deuktemsiru.common.today
import com.deuktemsiru.repository.CartItemRepository
import com.deuktemsiru.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CartService(
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val memberService: MemberService,
    private val clock: Clock,
) {

    @Transactional
    fun addToCart(memberId: Long, productId: Long, quantity: Int): CartItem {
        require(quantity > 0) { "장바구니 수량은 1개 이상이어야 합니다." }
        val today = clock.today()
        val member = memberService.findMember(memberId)
        require(member.role == MemberRole.CONSUMER) { "소비자 계정만 장바구니를 사용할 수 있습니다." }

        // B3: 재고 확인을 pessimistic lock 으로 수행하여 overselling 방지
        val product = productRepository.findByIdForUpdate(productId)
            .orNotFound("상품을 찾을 수 없습니다.")
        product.requirePurchasableOn(today)

        val existingItems = cartItemRepository.findByMemberOrderByCreatedAtAsc(member)
        val existing = cartItemRepository.findByMemberAndProduct(member, product).orElse(null)
        val nextQuantity = existing?.let { it.quantity + quantity } ?: quantity
        product.requirePurchasableOn(today, nextQuantity)

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
        val cartItem = findOwnedCartItem(memberId, cartItemId)
        cartItemRepository.delete(cartItem)
    }

    @Transactional
    fun updateQuantity(memberId: Long, cartItemId: Long, quantity: Int): CartItem {
        require(quantity > 0) { "장바구니 수량은 1개 이상이어야 합니다." }
        val cartItem = findOwnedCartItem(memberId, cartItemId)
        cartItem.product.requirePurchasableOn(clock.today(), quantity)
        cartItem.quantity = quantity
        return toResponse(cartItem)
    }

    @Transactional
    fun clearCart(memberId: Long) {
        val member = memberService.findMember(memberId)
        cartItemRepository.deleteByMember(member)
    }

    private fun findOwnedCartItem(memberId: Long, cartItemId: Long): CartItemEntity =
        cartItemRepository.findById(cartItemId)
            .orNotFound("장바구니 상품을 찾을 수 없습니다.")
            .also { require(it.member.memberId == memberId) { "접근 권한이 없습니다." } }

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

    private fun today(): LocalDate = clock.today()
}
