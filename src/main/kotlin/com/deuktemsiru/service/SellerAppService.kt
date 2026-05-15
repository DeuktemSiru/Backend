package com.deuktemsiru.service

import com.deuktemsiru.dto.*
import com.deuktemsiru.entity.BusinessInfo
import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Notification
import com.deuktemsiru.entity.NotificationType
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductImage
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import com.deuktemsiru.repository.BusinessInfoRepository
import com.deuktemsiru.repository.MenuItemRepository
import com.deuktemsiru.repository.NotificationRepository
import com.deuktemsiru.repository.OrderRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.StoreRepository
import com.deuktemsiru.repository.WishlistRepository
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
    private val orderRepository: OrderRepository,
    private val notificationRepository: NotificationRepository,
    private val businessInfoRepository: BusinessInfoRepository,
    private val wishlistRepository: WishlistRepository,
    private val memberService: MemberService,
    private val menuImageStorageService: MenuImageStorageService,
    private val fcmService: FcmService,
) {

    // ── 가게 조회/수정 ────────────────────────────────────────────────────────

    fun getStore(sellerId: Long): SellerStoreResponse {
        val store = sellerStore(sellerId)
        val today = LocalDate.now()
        val todayProductCount = productRepository.findByStoreAndAvailableDateAndStatus(
            store, today, ProductStatus.AVAILABLE
        ).size
        val pendingOrderCount = orderRepository.findByStoreOrderByCreatedAtDesc(store)
            .count { it.status == OrderStatus.PENDING }
        return SellerStoreResponse(
            storeId = store.storeId,
            name = store.name,
            isActive = store.isActive,
            isVerified = store.isVerified,
            todayProductCount = todayProductCount,
            pendingOrderCount = pendingOrderCount,
            ratingAvg = store.ratingAvg,
            reviewCount = store.reviewCount,
        )
    }

    @Transactional
    fun updateStore(sellerId: Long, req: SellerUpdateStoreRequest): SellerStoreResponse {
        val store = sellerStore(sellerId)
        req.description?.let { store.description = it }
        req.phone?.let { store.phone = it }
        return getStore(sellerId)
    }

    // ── 상품 관리 ─────────────────────────────────────────────────────────────

    fun getProducts(sellerId: Long, date: LocalDate? = null, status: String? = null): List<SellerSaleItemResponse> {
        val store = sellerStore(sellerId)
        val targetDate = date ?: LocalDate.now()
        var products = productRepository.findByStore(store)
            .filter { it.availableDate == targetDate }
            .sortedByDescending { it.createdAt }
        if (status != null) {
            val statusEnum = runCatching { ProductStatus.valueOf(status.uppercase()) }.getOrNull()
            if (statusEnum != null) products = products.filter { it.status == statusEnum }
        }
        return products.map { SellerSaleItemResponse.from(it) }
    }

    @Transactional
    fun createProduct(sellerId: Long, req: SaleItemRequest): SellerSaleItemResponse {
        return SellerSaleItemResponse.from(createProductEntity(sellerId, req))
    }

    private fun createProductEntity(sellerId: Long, req: SaleItemRequest): Product {
        require(req.quantityTotal > 0) { "판매 수량은 1개 이상이어야 합니다." }
        require(req.discountPrice > 0) { "할인가는 1원 이상이어야 합니다." }
        require(req.discountPrice < req.originalPrice) { "할인가는 정가보다 낮아야 합니다." }

        val store = sellerStore(sellerId)
        val menuItem = req.menuItemId?.let {
            menuItemRepository.findById(it)
                .orElseThrow { NoSuchElementException("메뉴를 찾을 수 없습니다.") }
                .also { mi -> require(mi.store.storeId == store.storeId) { "내 매장의 메뉴만 등록할 수 있습니다." } }
        }

        return productRepository.save(
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
                pickupStart = LocalTime.parse(req.pickupStart),
                pickupEnd = LocalTime.parse(req.pickupEnd),
                availableDate = LocalDate.parse(req.availableDate),
            )
        )
    }

    @Transactional
    fun createProductWithImages(
        sellerId: Long,
        req: SaleItemRequest,
        images: List<org.springframework.web.multipart.MultipartFile>,
    ): SellerSaleItemResponse {
        val product = createProductEntity(sellerId, req)
        images.filter { !it.isEmpty }.forEachIndexed { index, image ->
            val imageUrl = menuImageStorageService.save(image)
            if (imageUrl != null) {
                product.images += ProductImage(product = product, imageUrl = imageUrl, displayOrder = index)
            }
        }
        product.thumbnailUrl = product.images.minByOrNull { it.displayOrder }?.imageUrl ?: product.thumbnailUrl
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

    // ── 메뉴 마스터 관리 ──────────────────────────────────────────────────────

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

    // ── 사업자 정보 등록 ──────────────────────────────────────────────────────

    @Transactional
    fun registerBusinessInfo(sellerId: Long, businessNumber: String): BusinessInfo {
        if (businessInfoRepository.existsByBusinessNumber(businessNumber)) {
            throw IllegalStateException("이미 등록된 사업자 번호입니다.")
        }
        val member = memberService.findMember(sellerId)
        val businessInfo = BusinessInfo(
            member = member,
            businessName = businessNumber,   // 실제 인증 API 연동 전 임시값
            businessNumber = businessNumber,
            isVerified = true,               // 실제 인증 API 연동 전 임시 승인
            isSiruVerified = false,
        )
        return businessInfoRepository.save(businessInfo)
    }

    // ── 알림 발송 ─────────────────────────────────────────────────────────────

    @Transactional
    fun sendNotification(sellerId: Long, req: SendNotificationRequest): SellerNotificationResponse {
        require(req.message.isNotBlank()) { "알림 내용을 입력해 주세요." }
        val store = sellerStore(sellerId)
        val recipients = notificationRecipients(store, req)
        val saved = recipients.map { member ->
            notificationRepository.save(
                Notification(
                    member = member,
                    relatedStoreId = store.storeId,
                    type = NotificationType.EVENT,
                    title = store.name,
                    body = req.message,
                )
            ).also {
                fcmService.sendToMember(member.memberId, it.title, it.body)
            }
        }
        val sent = saved.firstOrNull()
        return SellerNotificationResponse(
            id = sent?.notificationId ?: 0,
            storeId = store.storeId,
            storeName = store.name,
            message = req.message,
            sentAt = (sent?.createdAt ?: java.time.LocalDateTime.now()).toString(),
            recipientCount = recipients.size,
        )
    }

    fun getSellerNotifications(sellerId: Long): List<SellerNotificationResponse> {
        val store = sellerStore(sellerId)
        return notificationRepository.findAll()
            .filter { it.relatedStoreId == store.storeId && it.title == store.name }
            .groupBy { it.body to it.createdAt.toString().take(16) }
            .map { (_, notifications) ->
                val first = notifications.maxBy { it.createdAt }
                SellerNotificationResponse(
                    id = first.notificationId,
                    storeId = store.storeId,
                    storeName = store.name,
                    message = first.body,
                    sentAt = first.createdAt.toString(),
                    recipientCount = notifications.size,
                )
            }
            .sortedByDescending { it.sentAt }
    }

    private fun notificationRecipients(store: Store, req: SendNotificationRequest): List<com.deuktemsiru.entity.Member> {
        val wishlistedMembers = wishlistRepository.findByStore(store)
            .map { it.member }
        val orderMembers = orderRepository.findByStoreOrderByCreatedAtDesc(store)
            .map { it.consumer }
        val base = when (req.targetType.uppercase()) {
            "NEARBY" -> (wishlistedMembers + orderMembers)
            else -> (wishlistedMembers + orderMembers)
        }
        return base
            .filter { it.status }
            .distinctBy { it.memberId }
    }

    private fun sellerStore(sellerId: Long) =
        storeRepository.findByOwner(memberService.findMember(sellerId))
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
}
