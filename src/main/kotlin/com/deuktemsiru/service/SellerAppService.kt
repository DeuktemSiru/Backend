package com.deuktemsiru.service

import com.deuktemsiru.dto.*
import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Notification
import com.deuktemsiru.entity.NotificationType
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.repository.MenuItemRepository
import com.deuktemsiru.repository.NotificationRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class SellerAppService(
    private val storeRepository: StoreRepository,
    private val menuItemRepository: MenuItemRepository,
    private val productRepository: ProductRepository,
    private val notificationRepository: NotificationRepository,
    private val memberService: MemberService,
    private val menuImageStorageService: MenuImageStorageService,
) {

    fun getProducts(sellerId: Long): List<SellerSaleItemResponse> {
        val store = sellerStore(sellerId)
        return productRepository.findByStore(store)
            .sortedByDescending { it.createdAt }
            .map { SellerSaleItemResponse.from(it) }
    }

    @Transactional
    fun createProduct(sellerId: Long, req: SaleItemRequest): SellerSaleItemResponse {
        require(req.discountRate in 1..99) { "할인율은 1~99 사이여야 합니다." }
        require(req.quantity > 0) { "판매 수량은 1개 이상이어야 합니다." }

        val store = sellerStore(sellerId)
        val menuItem = menuItemRepository.findById(req.menuItemId)
            .orElseThrow { NoSuchElementException("메뉴를 찾을 수 없습니다.") }
        require(menuItem.store.storeId == store.storeId) { "내 매장의 메뉴만 판매 상품으로 등록할 수 있습니다." }

        val (pickupStart, pickupEnd) = parsePickupTimeSlot(req.pickupTimeSlot)
        val product = productRepository.save(
            Product(
                store = store,
                menuItem = menuItem,
                name = menuItem.name,
                description = menuItem.description,
                thumbnailUrl = menuItem.imageUrl,
                originalPrice = menuItem.originalPrice,
                discountPrice = discountedPrice(menuItem.originalPrice, req.discountRate),
                quantityTotal = req.quantity,
                quantityRemaining = req.quantity,
                allergenInfo = menuItem.allergenInfo,
                pickupStart = pickupStart,
                pickupEnd = pickupEnd,
                availableDate = LocalDate.now(),
            )
        )
        return SellerSaleItemResponse.from(product)
    }

    @Transactional
    fun updateProductStatus(sellerId: Long, productId: Long, req: UpdateSaleStatusRequest): SellerSaleItemResponse {
        val store = sellerStore(sellerId)
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("판매 상품을 찾을 수 없습니다.") }
        require(product.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        product.status = parseProductStatus(req.status)
        if (product.status == ProductStatus.SOLD_OUT) product.quantityRemaining = 0
        if (product.status == ProductStatus.AVAILABLE && product.quantityRemaining == 0) {
            product.quantityRemaining = product.quantityTotal
        }
        return SellerSaleItemResponse.from(product)
    }

    @Transactional
    fun deleteProduct(sellerId: Long, productId: Long) {
        val store = sellerStore(sellerId)
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("판매 상품을 찾을 수 없습니다.") }
        require(product.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        product.status = ProductStatus.CANCELLED
        product.quantityRemaining = 0
    }

    fun getMenus(sellerId: Long): List<SellerMenuItemResponse> {
        val store = sellerStore(sellerId)
        return menuItemRepository.findByStoreAndIsActiveTrue(store).map { SellerMenuItemResponse.from(it) }
    }

    @Transactional
    fun createMenu(sellerId: Long, req: SellerMenuItemRequest, imageUrl: String? = null): SellerMenuItemResponse {
        require(req.name.isNotBlank()) { "메뉴 이름을 입력해 주세요." }
        require(req.originalPrice > 0) { "정상가는 1원 이상이어야 합니다." }
        val store = sellerStore(sellerId)
        val menuItem = menuItemRepository.save(
            MenuItem(
                store = store,
                name = req.name,
                originalPrice = req.originalPrice,
                imageUrl = imageUrl,
                allergenInfo = req.allergyInfo,
            )
        )
        return SellerMenuItemResponse.from(menuItem)
    }

    @Transactional
    fun createMenuWithImage(
        sellerId: Long,
        name: String,
        originalPrice: Int,
        allergyInfo: String?,
        discountRate: Int?,
        quantity: Int?,
        pickupTimeSlot: String?,
        image: org.springframework.web.multipart.MultipartFile?,
    ): SellerMenuItemResponse {
        val imageUrl = menuImageStorageService.save(image)
        val menu = createMenu(
            sellerId,
            SellerMenuItemRequest(name = name, originalPrice = originalPrice, allergyInfo = allergyInfo),
            imageUrl,
        )
        if (discountRate != null && quantity != null && pickupTimeSlot != null) {
            createProduct(
                sellerId,
                SaleItemRequest(
                    menuItemId = menu.id,
                    discountRate = discountRate,
                    quantity = quantity,
                    pickupTimeSlot = pickupTimeSlot,
                )
            )
        }
        return menu
    }

    @Transactional
    fun updateMenu(sellerId: Long, menuItemId: Long, req: SellerMenuItemUpdateRequest): SellerMenuItemResponse {
        val store = sellerStore(sellerId)
        val menuItem = menuItemRepository.findById(menuItemId)
            .orElseThrow { NoSuchElementException("메뉴를 찾을 수 없습니다.") }
        require(menuItem.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        req.name?.takeIf { it.isNotBlank() }?.let { menuItem.name = it }
        req.originalPrice?.let {
            require(it > 0) { "정상가는 1원 이상이어야 합니다." }
            menuItem.originalPrice = it
        }
        return SellerMenuItemResponse.from(menuItem)
    }

    @Transactional
    fun deleteMenu(sellerId: Long, menuItemId: Long) {
        val store = sellerStore(sellerId)
        val menuItem = menuItemRepository.findById(menuItemId)
            .orElseThrow { NoSuchElementException("메뉴를 찾을 수 없습니다.") }
        require(menuItem.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        menuItem.isActive = false
    }

    fun getStore(sellerId: Long): SellerStoreResponse = SellerStoreResponse.from(sellerStore(sellerId))

    @Transactional
    fun updateStore(sellerId: Long, req: SellerUpdateStoreRequest): SellerStoreResponse {
        val store = sellerStore(sellerId)
        req.address?.takeIf { it.isNotBlank() }?.let { store.address = it }
        req.phone?.let { store.phone = it }
        return SellerStoreResponse.from(store)
    }

    @Transactional
    fun sendNotification(sellerId: Long, req: SendNotificationRequest): SellerNotificationResponse {
        require(req.message.isNotBlank()) { "알림 내용을 입력해 주세요." }
        val store = sellerStore(sellerId)
        val sent = notificationRepository.save(
            Notification(
                member = store.owner,
                relatedStoreId = store.storeId,
                type = NotificationType.EVENT,
                title = store.name,
                body = req.message,
            )
        )
        return SellerNotificationResponse(
            id = sent.notificationId,
            storeId = store.storeId,
            storeName = store.name,
            message = sent.body,
            sentAt = sent.createdAt.toString(),
            recipientCount = 1,
        )
    }

    fun getNotifications(sellerId: Long): List<SellerNotificationResponse> {
        val store = sellerStore(sellerId)
        return notificationRepository.findByMemberOrderByCreatedAtDesc(store.owner)
            .filter { it.relatedStoreId == store.storeId }
            .map {
                SellerNotificationResponse(
                    id = it.notificationId,
                    storeId = it.relatedStoreId ?: store.storeId,
                    storeName = it.title,
                    message = it.body,
                    sentAt = it.createdAt.toString(),
                    recipientCount = 1,
                )
            }
    }

    private fun sellerStore(sellerId: Long) =
        storeRepository.findByOwner(memberService.findMember(sellerId))
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
}
