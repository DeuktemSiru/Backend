package com.deuktemsiru

import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.OrderItemRequest
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.entity.changeSaleStatus
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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.hamcrest.Matchers.containsString

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class DeuktemsiruApplicationTests {

    companion object {
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
    }

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

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun contextLoads() {
    }

    @Test
    fun `openapi json is publicly available and documents core api groups`() {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.info.title").value("Deuktemsiru API"))
            .andExpect(content().string(containsString("/api/v1/auth/debug/login")))
            .andExpect(content().string(containsString("/api/v1/products")))
            .andExpect(content().string(containsString("/api/v1/cart")))
            .andExpect(content().string(containsString("Buyer Products")))
            .andExpect(content().string(containsString("Seller Orders")))
    }

    @Test
    fun `protected buyer api rejects requests without bearer token`() {
        mockMvc.perform(get("/api/v1/cart"))
            .andExpect { result ->
                assertTrue(
                    result.response.status == 401 || result.response.status == 403,
                    "Expected unauthenticated cart request to be rejected, got ${result.response.status}",
                )
            }
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
    fun `manual pause preserves stock so seller can reopen sale`() {
        val store = storeRepository.findAll().first()
        val product = productRepository.findByStore(store).first()
        val originalStock = product.quantityRemaining

        product.changeSaleStatus(ProductStatus.PAUSED)
        assertEquals(ProductStatus.PAUSED, product.status)
        assertEquals(originalStock, product.quantityRemaining)

        product.changeSaleStatus(ProductStatus.AVAILABLE)
        assertEquals(ProductStatus.AVAILABLE, product.status)
        assertEquals(originalStock, product.quantityRemaining)
    }

    @Test
    fun `seller cannot manually mark product sold out without changing stock`() {
        val store = storeRepository.findAll().first()
        val product = productRepository.findByStore(store).first()
        val originalStock = product.quantityRemaining

        assertThrows(IllegalArgumentException::class.java) {
            product.changeSaleStatus(ProductStatus.SOLD_OUT)
        }

        assertEquals(ProductStatus.AVAILABLE, product.status)
        assertEquals(originalStock, product.quantityRemaining)
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
