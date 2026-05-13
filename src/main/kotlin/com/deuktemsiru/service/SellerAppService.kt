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
import java.time.LocalTime

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
        require(req.quantityTotal > 0) { "판매 수량은 1개 이상이어야 합니다." }
        require(req.discountPrice > 0) { "할인가는 1원 이상이어야 합니다." }
        require(req.discountPrice < req.originalPrice) { "할인가는 정가보다 낮아야 합니다." }

        val store = sellerStore(sellerId)
        val menuItem = req.menuItemId?.let {
            menuItemRepository.findById(it)
                .orElseThrow { NoSuchElementException("메뉴를 찾을 수 없습니다.") }
                .also { mi -> require(mi.store.storeId == store.storeId) { "내 매장의 메뉴만 판매 상품으로 등록할 수 있습니다." } }
        }

        val pickupStart = LocalTime.parse(req.pickupStart)
        val pickupEnd   = LocalTime.parse(req.pickupEnd)
        val availableDate = LocalDate.parse(req.availableDate)

        val product = productRepository.save(
            Product(
                store = store,
                menuItem = menuItem,
                name = req.name,
                description = menuItem?.description,
                thumbnailUrl = menuItem?.imageUrl,
                originalPrice = req.originalPrice,
                discountPrice = req.discountPrice,
                quantityTotal = req.quantityTotal,
                quantityRemaining = req.quantityTotal,
                allergenInfo = req.allergenInfo ?: menuItem?.allergenInfo,
                madeAt = req.madeAt,
                pickupStart = pickupStart,
                pickupEnd = pickupEnd,
                availableDate = availableDate,
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
        return SellerSaleItemResponse.from(product)
    }

    @Transactional
    fun deleteProduct(sellerId: Long, productId: Long) {
        val store = sellerStore(sellerId)
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("판매 상품을 찾을 수 없습니다.") }
        require(product.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        productRepository.delete(product)
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
                allergenInfo = req.allergenInfo,
            )
        )
        return SellerMenuItemResponse.from(menuItem)
    }

    @Transactional
    fun createMenuWithImage(
        sellerId: Long,
        name: String,
        description: String?,
        originalPrice: Int,
        allergenInfo: String?,
        image: org.springframework.web.multipart.MultipartFile?,
    ): SellerMenuItemResponse {
        val imageUrl = menuImageStorageService.save(image)
        return createMenu(
            sellerId,
            SellerMenuItemRequest(name = name, description = description, originalPrice = originalPrice, allergenInfo = allergenInfo),
            imageUrl,
        )
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

    private fun sellerStore(sellerId: Long) =
        storeRepository.findByOwner(memberService.findMember(sellerId))
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
}
                                                                                                                                   