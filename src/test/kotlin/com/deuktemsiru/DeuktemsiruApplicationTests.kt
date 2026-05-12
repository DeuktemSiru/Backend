package com.deuktemsiru

import com.deuktemsiru.dto.CreateOrderRequest
import com.deuktemsiru.dto.MenuItemUpdateRequest
import com.deuktemsiru.dto.OrderItemRequest
import com.deuktemsiru.dto.SendNotificationRequest
import com.deuktemsiru.dto.UpdateOrderStatusRequest
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.repository.MenuItemRepository
import com.deuktemsiru.repository.StoreRepository
import com.deuktemsiru.repository.UserRepository
import com.deuktemsiru.service.NotificationService
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
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var menuItemRepository: MenuItemRepository

    @Test
    fun contextLoads() {
    }

    @Test
    fun `order creation rejects menu from another store`() {
        val buyer = userRepository.findByEmail("buyer@test.com").get()
        val stores = storeRepository.findAll()
        val store = stores[0]
        val otherStoreMenu = stores[1].menuItems.first()

        val error = assertThrows(IllegalArgumentException::class.java) {
            orderService.createOrder(
                buyer.id,
                CreateOrderRequest(
                    storeId = store.id,
                    items = listOf(OrderItemRequest(menuItemId = otherStoreMenu.id, quantity = 1)),
                    pickupTime = "18:00",
                ),
            )
        }

        assertEquals("선택한 가게의 메뉴만 주문할 수 있습니다.", error.message)
    }

    @Test
    fun `order creation rejects invalid or excessive quantity without changing stock`() {
        val buyer = userRepository.findByEmail("buyer@test.com").get()
        val store = storeRepository.findAll().first()
        val menu = store.menuItems.first()
        val originalStock = menu.remainingItems

        assertThrows(IllegalArgumentException::class.java) {
            orderService.createOrder(
                buyer.id,
                CreateOrderRequest(
                    storeId = store.id,
                    items = listOf(OrderItemRequest(menuItemId = menu.id, quantity = 0)),
                    pickupTime = "18:00",
                ),
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            orderService.createOrder(
                buyer.id,
                CreateOrderRequest(
                    storeId = store.id,
                    items = listOf(OrderItemRequest(menuItemId = menu.id, quantity = originalStock + 1)),
                    pickupTime = "18:00",
                ),
            )
        }

        val reloaded = menuItemRepository.findById(menu.id).get()
        assertEquals(originalStock, reloaded.remainingItems)
        assertFalse(reloaded.isSoldOut)
    }

    @Test
    fun `order creation decrements stock and marks sold out`() {
        val buyer = userRepository.findByEmail("buyer@test.com").get()
        val store = storeRepository.findAll().first()
        val menu = store.menuItems.first()

        val order = orderService.createOrder(
            buyer.id,
            CreateOrderRequest(
                storeId = store.id,
                items = listOf(OrderItemRequest(menuItemId = menu.id, quantity = menu.remainingItems)),
                pickupTime = "18:00",
            ),
        )

        val reloaded = menuItemRepository.findById(menu.id).get()
        assertEquals(OrderStatus.NEW, order.status)
        assertEquals(0, reloaded.remainingItems)
        assertTrue(reloaded.isSoldOut)
    }

    @Test
    fun `seller order status follows allowed transitions`() {
        val buyer = userRepository.findByEmail("buyer@test.com").get()
        val seller = userRepository.findByEmail("bakery@test.com").get()
        val store = storeRepository.findByOwner(seller).get()
        val menu = store.menuItems.first()
        val order = orderService.createOrder(
            buyer.id,
            CreateOrderRequest(
                storeId = store.id,
                items = listOf(OrderItemRequest(menuItemId = menu.id, quantity = 1)),
                pickupTime = "18:00",
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            orderService.updateOrderStatus(seller.id, order.id, UpdateOrderStatusRequest(OrderStatus.COMPLETED))
        }

        assertEquals(
            OrderStatus.PREPARING,
            orderService.updateOrderStatus(seller.id, order.id, UpdateOrderStatusRequest(OrderStatus.PREPARING)).status,
        )
        assertEquals(
            OrderStatus.READY,
            orderService.updateOrderStatus(seller.id, order.id, UpdateOrderStatusRequest(OrderStatus.READY)).status,
        )
        assertEquals(
            OrderStatus.COMPLETED,
            orderService.updateOrderStatus(seller.id, order.id, UpdateOrderStatusRequest(OrderStatus.COMPLETED)).status,
        )

        assertThrows(IllegalArgumentException::class.java) {
            orderService.updateOrderStatus(seller.id, order.id, UpdateOrderStatusRequest(OrderStatus.REJECTED))
        }
    }

    @Test
    fun `buyer notification list contains notifications from wishlisted stores`() {
        val buyer = userRepository.findByEmail("buyer@test.com").get()
        val seller = userRepository.findByEmail("bakery@test.com").get()
        val store = storeRepository.findByOwner(seller).get()

        assertTrue(storeService.toggleWishlist(buyer.id, store.id))

        val sent = notificationService.send(seller.id, SendNotificationRequest("오늘 마감 특가가 열렸어요"))
        val notifications = notificationService.getBuyerNotifications(buyer.id)

        assertEquals(1, sent.recipientCount)
        assertTrue(notifications.any { it.id == sent.id && it.storeId == store.id })
    }

    @Test
    fun `seller can update and delete own menu`() {
        val seller = userRepository.findByEmail("bakery@test.com").get()
        val store = storeRepository.findByOwner(seller).get()
        val menu = store.menuItems.first()

        val updated = storeService.updateMenuItem(
            seller.id,
            menu.id,
            MenuItemUpdateRequest(
                remainingItems = 2,
                discountRate = 50,
                pickupTimeSlot = "18:00-19:00",
            ),
        )

        assertEquals(2, updated.remainingItems)
        assertEquals(50, updated.discountRate)
        assertEquals("18:00-19:00", updated.pickupTimeSlot)

        storeService.deleteMenuItem(seller.id, menu.id)

        assertFalse(store.menuItems.any { it.id == menu.id })
    }
}
