package com.deuktemsiru

import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.OrderItemRequest
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import com.deuktemsiru.repository.MemberRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.StoreRepository
import com.deuktemsiru.service.OrderService
import com.deuktemsiru.service.ReviewCreateRequest
import com.deuktemsiru.service.ReviewService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 결제·픽업·리뷰 경로 통합 테스트. (PLAN P0-2)
 * 롤백과 동시성을 실제로 확인해야 해서 @Transactional 없이 매번 커밋한다.
 * 테스트끼리 간섭하지 않도록 상품과 구매자는 테스트마다 새로 만든다.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OrderPaymentIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
    }

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var reviewService: ReviewService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Test
    fun `주문에서 픽업 코드 검증까지 정상 흐름이 성립한다`() {
        val seller = seller()
        val store = storeOf(seller)
        val product = newProduct(store, quantity = 2, price = 7_000)
        val consumer = newConsumer(balance = 20_000, siruLinked = true)

        val order = orderService.createOrder(
            consumer.memberId,
            CreateOrderRequest(items = listOf(OrderItemRequest(product.productId, 2)), paymentMethod = "SIRU"),
        )
        assertEquals(14_000, order.totalPrice)
        assertEquals(6_000, memberRepository.findById(consumer.memberId).get().siruBalance)
        assertEquals(0, productRepository.findById(product.productId).get().quantityRemaining)

        orderService.updateOrderStatus(seller.memberId, order.orderId, UpdateOrderStatusRequest(OrderStatus.CONFIRMED))

        // 코드가 틀리면 픽업이 성립하지 않는다.
        assertThrows(NoSuchElementException::class.java) {
            orderService.confirmPickupCode(seller.memberId, order.orderId, "WRONGCODE")
        }

        val pickedUp = orderService.confirmPickupCode(seller.memberId, order.orderId, order.pickupCode!!)
        assertEquals(OrderStatus.PICKED_UP, pickedUp.status)
    }

    @Test
    fun `시루 잔액이 부족하면 주문이 거절되고 재고와 잔액이 그대로 남는다`() {
        val product = newProduct(storeOf(seller()), quantity = 3, price = 10_000)
        val consumer = newConsumer(balance = 9_999, siruLinked = true)

        val error = assertThrows(IllegalArgumentException::class.java) { orderSiru(consumer, product) }

        assertEquals("시루 잔액이 부족합니다.", error.message)
        assertEquals(3, productRepository.findById(product.productId).get().quantityRemaining)
        assertEquals(9_999, memberRepository.findById(consumer.memberId).get().siruBalance)
    }

    @Test
    fun `시루 미연동 계정은 시루 결제로 주문할 수 없다`() {
        val product = newProduct(storeOf(seller()), quantity = 3, price = 10_000)
        val consumer = newConsumer(balance = 50_000, siruLinked = false)

        val error = assertThrows(IllegalArgumentException::class.java) { orderSiru(consumer, product) }

        assertEquals("시루 계정 연동이 필요합니다.", error.message)
        assertEquals(3, productRepository.findById(product.productId).get().quantityRemaining)
    }

    @Test
    fun `동시 주문은 재고를 초과 판매하지 않는다`() {
        val stock = 5
        val attempts = 12
        val product = newProduct(storeOf(seller()), quantity = stock, price = 5_000)
        val consumers = (1..attempts).map { newConsumer() }

        val pool = Executors.newFixedThreadPool(attempts)
        val succeeded = try {
            pool.invokeAll(
                consumers.map { consumer ->
                    Callable {
                        runCatching {
                            orderService.createOrder(
                                consumer.memberId,
                                CreateOrderRequest(items = listOf(OrderItemRequest(product.productId, 1))),
                            )
                        }.isSuccess
                    }
                },
            ).count { it.get() }
        } finally {
            pool.shutdown()
            pool.awaitTermination(60, TimeUnit.SECONDS)
        }

        val reloaded = productRepository.findById(product.productId).get()
        assertEquals(stock, succeeded)
        assertEquals(0, reloaded.quantityRemaining)
        assertEquals(ProductStatus.SOLD_OUT, reloaded.status)
    }

    @Test
    fun `리뷰는 픽업 완료된 본인 주문에 1건만 쓸 수 있다`() {
        val seller = seller()
        val store = storeOf(seller)
        val product = newProduct(store, quantity = 2, price = 7_000)
        val consumer = newConsumer()
        val order = orderService.createOrder(
            consumer.memberId,
            CreateOrderRequest(items = listOf(OrderItemRequest(product.productId, 1))),
        )
        val review = ReviewCreateRequest(storeId = store.storeId, orderId = order.orderId, rating = 5, content = "좋았습니다")

        assertEquals(
            "픽업 완료된 주문에만 리뷰를 작성할 수 있습니다.",
            assertThrows(IllegalArgumentException::class.java) {
                reviewService.createReview(consumer.memberId, review)
            }.message,
        )

        orderService.updateOrderStatus(seller.memberId, order.orderId, UpdateOrderStatusRequest(OrderStatus.CONFIRMED))
        orderService.updateOrderStatus(seller.memberId, order.orderId, UpdateOrderStatusRequest(OrderStatus.PICKED_UP))

        reviewService.createReview(consumer.memberId, review)

        assertEquals(
            "이미 작성한 리뷰가 있습니다.",
            assertThrows(IllegalArgumentException::class.java) {
                reviewService.createReview(consumer.memberId, review.copy(rating = 1))
            }.message,
        )
        assertEquals(
            "본인의 주문에만 리뷰를 작성할 수 있습니다.",
            assertThrows(IllegalArgumentException::class.java) {
                reviewService.createReview(newConsumer().memberId, review)
            }.message,
        )
        assertEquals(
            "별점은 1~5 사이여야 합니다.",
            assertThrows(IllegalArgumentException::class.java) {
                reviewService.createReview(consumer.memberId, review.copy(rating = 6))
            }.message,
        )
    }

    // ── 테스트 데이터 ────────────────────────────────────────────────────────

    private fun seller(): Member = memberRepository.findByEmail("bakery@test.com").orElseThrow()

    private fun storeOf(seller: Member): Store = storeRepository.findByOwner(seller).orElseThrow()

    private fun orderSiru(consumer: Member, product: Product) = orderService.createOrder(
        consumer.memberId,
        CreateOrderRequest(items = listOf(OrderItemRequest(product.productId, 1)), paymentMethod = "SIRU"),
    )

    private fun newConsumer(balance: Int = 0, siruLinked: Boolean = false): Member {
        val unique = UUID.randomUUID().toString()
        return memberRepository.save(
            Member(
                providerId = "kakao_$unique",
                email = "$unique@test.local",
                name = "테스트구매자",
                nickname = unique.take(8),
                role = MemberRole.CONSUMER,
                isSiruLinked = siruLinked,
                siruBalance = balance,
            ),
        )
    }

    private fun newProduct(store: Store, quantity: Int, price: Int): Product =
        productRepository.save(
            Product(
                store = store,
                name = "테스트상품 ${UUID.randomUUID()}",
                originalPrice = price * 2,
                discountPrice = price,
                quantityTotal = quantity,
                quantityRemaining = quantity,
                pickupStart = LocalTime.of(0, 0),
                pickupEnd = LocalTime.of(23, 59),
                availableDate = LocalDate.now(),
            ),
        )
}
