package com.deuktemsiru

import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.OrderItemRequest
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.repository.MemberRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.StoreRepository
import com.deuktemsiru.service.OrderService
import com.deuktemsiru.service.StoreService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class DeuktemsiruApplicationTests {

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var storeService: StoreService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Test
    fun contextLoads() {
    }

    @Test
    fun `order creation rejects mixing products from different stores`() {
        val buyer = memberRepository.findByEmail("buyer@test.com").get()
        val stores = storeRepository.findAll()
        val firstStoreProduct = productRepository.findByStore(stores[0]).first()
        val otherStoreProduct = productRepository.findByStore(stores[1]).first()

        val error = assertThrows(IllegalArgumentException::class.java) {
            orderService.createOrder(
                buyer.memberId,
                CreateOrderRequest(
                    items = listOf(
                        OrderItemRequest(productId = firstStoreProduct.productId, quantity = 1),
                        OrderItemRequest(productId = otherStoreProduct.productId, quantity = 1),
                    ),
                ),
            )
        }

        assertEquals("같은 가게의 상품만 주문할 수 있습니다.", error.message)
    }

    @Test
    fun `order creation rejects invalid or excessive quantity without changing stock`() {
        val buyer = memberRepository.findByEmail("buyer@test.com").get()
        val store = storeRepository.findAll().first()
        val product = productRepository.findByStore(store).first()
        val originalStock = product.quantityRemaining

        assertThrows(IllegalArgumentException::class.java) {
            orderService.createOrder(
                buyer.memberId,
                CreateOrderRequest(
                    items = listOf(OrderItemRequest(productId = product.productId, quantity = 0)),
                ),
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            orderService.createOrder(
                buyer.memberId,
                CreateOrderRequest(
                    items = listOf(OrderItemRequest(productId = product.productId, quantity = originalStock + 1)),
                ),
            )
        }

        val reloaded = productRepository.findById(product.productId).get()
        assertEquals(originalStock, reloaded.quantityRemaining)
        assertEquals(ProductStatus.AVAILABLE, reloaded.status)
    }

    @Test
    fun `order creation decrements stock and marks sold out`() {
        val buyer = memberRepository.findByEmail("buyer@test.com").get()
        val store = storeRepository.findAll().first()
        val product = productRepository.findByStore(store).first()

        val order = orderService.createOrder(
            buyer.memberId,
            CreateOrderRequest(
                items = listOf(OrderItemRequest(productId = product.productId, quantity = product.quantityRemaining)),
            ),
        )

        val reloaded = productRepository.findById(product.productId).get()
        assertEquals(OrderStatus.PENDING, order.status)
        assertEquals(0, reloaded.quantityRemaining)
        assertEquals(ProductStatus.SOLD_OUT, reloaded.status)
    }

    @Test
    fun `seller order status follows allowed transitions`() {
        val buyer = memberRepository.findByEmail("buyer@test.com").get()
        val seller = memberRepository.findByEmail("bakery@test.com").get()
        val store = storeRepository.findByOwner(seller).get()
        val product = productRepository.findByStore(store).first()
        val order = orderService.createOrder(
            buyer.memberId,
            CreateOrderRequest(
                items = listOf(OrderItemRequest(productId = product.productId, quantity = 1)),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            orderService.updateOrderStatus(seller.memberId, order.orderId, UpdateOrderStatusRequest(OrderStatus.PICKED_UP))
        }

        assertEquals(
            OrderStatus.CONFIRMED,
            orderService.updateOrderStatus(seller.memberId, order.orderId, UpdateOrderStatusRequest(OrderStatus.CONFIRMED)).status,
        )
        assertEquals(
            OrderStatus.PICKED_UP,
            orderService.updateOrderStatus(seller.memberId, order.orderId, UpdateOrderStatusRequest(OrderStatus.PICKED_UP)).status,
        )

        assertThrows(IllegalArgumentException::class.java) {
            orderService.updateOrderStatus(seller.memberId, order.orderId, UpdateOrderStatusRequest(OrderStatus.CANCELLED))
        }
    }

    @Test
    fun `buyer can toggle wishlist`() {
        val buyer = memberRepository.findByEmail("buyer@test.com").get()
        val store = storeRepository.findAll().first()

        assertTrue(storeService.toggleWishlist(buyer.memberId, store.storeId))
        assertTrue(storeService.getWishlistBuyer(buyer.memberId).any { it.storeId == store.storeId })

        assertFalse(storeService.toggleWishlist(buyer.memberId, store.storeId))
        assertFalse(storeService.getWishlistBuyer(buyer.memberId).any { it.storeId == store.storeId })
    }
}
