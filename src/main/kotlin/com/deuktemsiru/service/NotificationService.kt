package com.deuktemsiru.service

import com.deuktemsiru.dto.NotificationResponse
import com.deuktemsiru.dto.SendNotificationRequest
import com.deuktemsiru.entity.Notification
import com.deuktemsiru.entity.UserRole
import com.deuktemsiru.repository.NotificationRepository
import com.deuktemsiru.repository.StoreRepository
import com.deuktemsiru.repository.WishlistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val storeRepository: StoreRepository,
    private val wishlistRepository: WishlistRepository,
    private val userService: UserService,
) {

    @Transactional
    fun send(sellerId: Long, req: SendNotificationRequest): NotificationResponse {
        require(req.message.length <= 40) { "메시지는 40자 이하여야 합니다." }

        val seller = userService.findUser(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }

        val recipientCount = wishlistRepository.countByStore(store).toInt()
        val notification = Notification(
            store = store,
            message = req.message,
            recipientCount = recipientCount,
        )
        return NotificationResponse.from(notificationRepository.save(notification))
    }

    fun getHistory(sellerId: Long): List<NotificationResponse> {
        val seller = userService.findUser(sellerId)
        val store = storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
        return notificationRepository.findByStoreOrderBySentAtDesc(store).map { NotificationResponse.from(it) }
    }

    fun getBuyerNotifications(userId: Long): List<NotificationResponse> {
        val buyer = userService.findUser(userId)
        require(buyer.role == UserRole.BUYER) { "구매자 계정만 알림을 조회할 수 있습니다." }

        val wishlistedStores = wishlistRepository.findByUser(buyer).map { it.store }
        if (wishlistedStores.isEmpty()) return emptyList()

        return notificationRepository.findByStoreInOrderBySentAtDesc(wishlistedStores)
            .map { NotificationResponse.from(it) }
    }
}
