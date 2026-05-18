package com.deuktemsiru.service

import com.deuktemsiru.dto.*
import com.deuktemsiru.entity.BusinessInfo
import com.deuktemsiru.entity.MemberRole
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
import com.deuktemsiru.repository.WishlistRepository
import com.deuktemsiru.common.toLocalDateOrThrow
import com.deuktemsiru.common.toLocalTimeOrThrow
import com.deuktemsiru.common.nowDateTime
import com.deuktemsiru.common.today
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class SellerAppService(
    private val menuItemRepository: MenuItemRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val notificationRepository: NotificationRepository,
    private val businessInfoRepository: BusinessInfoRepository,
    private val wishlistRepository: WishlistRepository,
    private val memberService: MemberService,
    private val menuImageStorageService: MenuImageStorageService,
    private val fcmService: FcmService,
    private val storeOwnershipService: StoreOwnershipService,
    private val clock: Clock,
) {

    // ── 가게 조회/수정 ────────────────────────────────────────────────────────

    fun getStore(sellerId: Long): SellerStoreResponse {
        val store = sellerStore(sellerId)
        val today = today()
        val todayProductCount = productRepository.countByStoreAndAvailableDateAndStatus(
            store, today, ProductStatus.AVAILABLE
        )
        val pendingOrderCount = orderRepository.countByStoreAndStatus(store, OrderStatus.PENDING).toInt()
        return SellerStoreResponse(
            storeId = store.storeId,
            name = store.name,
            address = store.address,
            phone = store.phone,
            closingTime = store.closingTime,
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
        req.address?.takeIf { it.isNotBlank() }?.let { store.address = it }
        req.phone?.let { store.phone = it }
        req.closingTime?.takeIf { it.isNotBlank() }?.let {
            parsePickupTime(it, "마감 시간")
            store.closingTime = it
        }
        return getStore(sellerId)
    }

    // ── 상품 관리 ─────────────────────────────────────────────────────────────

    fun getProducts(sellerId: Long, date: LocalDate? = null, status: String? = null): List<SellerSaleItemResponse> {
        val store = sellerStoreOrNull(sellerId) ?: return emptyList()
        val targetDate = date ?: today()
        val products = if (status != null) {
            val statusEnum = status.toProductStatus()
            productRepository.findByStoreAndAvailableDateAndStatus(store, targetDate, statusEnum)
        } else {
            productRepository.findByStoreAndAvailableDateOrderByCreatedAtDesc(store, targetDate)
                .filter { it.status != ProductStatus.DELETED }
        }
        return products.map { SellerSaleItemResponse.from(it) }
    }

    @Transactional
    fun createProduct(sellerId: Long, req: SaleItemRequest): SellerSaleItemResponse {
        return SellerSaleItemResponse.from(createProductEntity(sellerId, req))
    }

    private fun createProductEntity(sellerId: Long, req: SaleItemRequest): Product {
        require(req.name.isNotBlank()) { "상품 이름을 입력해 주세요." }
        require(req.originalPrice > 0) { "정상가는 1원 이상이어야 합니다." }
        require(req.quantityTotal > 0) { "판매 수량은 1개 이상이어야 합니다." }
        require(req.discountPrice > 0) { "할인가는 1원 이상이어야 합니다." }
        require(req.discountPrice < req.originalPrice) { "할인가는 정가보다 낮아야 합니다." }

        val store = sellerStore(sellerId)
        val menuItem = req.menuItemId?.let { findSellerMenu(store, it) }

        val pickupStart = parsePickupTime(req.pickupStart, "픽업 시작 시간")
        val pickupEnd = parsePickupTime(req.pickupEnd, "픽업 종료 시간")
        require(pickupEnd.isAfter(pickupStart)) { "픽업 종료 시간은 시작 시간보다 늦어야 합니다." }

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
                pickupStart = pickupStart,
                pickupEnd = pickupEnd,
                availableDate = parseAvailableDate(req.availableDate),
            )
        )
    }

    @Transactional
    fun createProductWithImages(
        sellerId: Long,
        req: SaleItemRequest,
        images: List<MultipartFile>,
    ): SellerSaleItemResponse {
        val product = createProductEntity(sellerId, req)
        savedImageUrls(images).forEachIndexed { index, imageUrl ->
            product.images += ProductImage(product = product, imageUrl = imageUrl, displayOrder = index)
        }
        product.thumbnailUrl = product.images.minByOrNull { it.displayOrder }?.imageUrl ?: product.thumbnailUrl
        return SellerSaleItemResponse.from(product)
    }

    @Transactional
    fun updateProductStatus(sellerId: Long, productId: Long, req: UpdateSaleStatusRequest): SellerSaleItemResponse {
        val store = sellerStore(sellerId)
        val product = findSellerProduct(store, productId)
        val nextStatus = req.status.toProductStatus()
        require(product.status != ProductStatus.DELETED) { "삭제된 상품은 상태를 변경할 수 없습니다." }
        require(nextStatus != ProductStatus.DELETED) { "상품 삭제는 삭제 API를 사용해 주세요." }
        if (nextStatus == ProductStatus.AVAILABLE && product.quantityRemaining <= 0) {
            throw IllegalStateException("잔여 수량이 없는 상품은 먼저 수량을 수정해 주세요.")
        }
        product.status = nextStatus
        if (nextStatus == ProductStatus.SOLD_OUT) product.quantityRemaining = 0
        return SellerSaleItemResponse.from(product)
    }

    @Transactional
    fun updateProduct(sellerId: Long, productId: Long, req: UpdateSaleItemRequest): SellerSaleItemResponse {
        val store = sellerStore(sellerId)
        val product = findSellerProduct(store, productId)
        require(product.status != ProductStatus.DELETED) { "삭제된 상품은 수정할 수 없습니다." }

        val nextOriginalPrice = req.originalPrice ?: product.originalPrice
        val nextDiscountPrice = req.discountPrice ?: product.discountPrice
        req.originalPrice?.let { require(it > 0) { "정상가는 1원 이상이어야 합니다." } }
        req.discountPrice?.let { require(it > 0) { "할인가는 1원 이상이어야 합니다." } }
        // B8: null 병합 후에도 두 값 모두 양수이고 할인가 < 정상가 조건을 명시적으로 검증
        require(nextDiscountPrice > 0 && nextOriginalPrice > 0 && nextDiscountPrice < nextOriginalPrice) {
            "할인가는 정가보다 낮아야 합니다."
        }
        product.originalPrice = nextOriginalPrice
        product.discountPrice = nextDiscountPrice

        req.quantityRemaining?.let {
            require(it >= 0) { "잔여 수량은 0개 이상이어야 합니다." }
            require(it <= product.quantityTotal) { "잔여 수량은 총 수량보다 많을 수 없습니다." }
            product.quantityRemaining = it
            product.status = if (it == 0) ProductStatus.SOLD_OUT else ProductStatus.AVAILABLE
        }
        return SellerSaleItemResponse.from(product)
    }

    @Transactional
    fun deleteProduct(sellerId: Long, productId: Long) {
        val store = sellerStore(sellerId)
        val product = findSellerProduct(store, productId)
        val activeStatuses = setOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)
        val hasActiveOrders = orderRepository.existsActiveOrderForProduct(store, activeStatuses.toList(), product)
        require(!hasActiveOrders) { "진행 중인 주문이 있는 상품은 취소할 수 없습니다." }
        product.status = ProductStatus.DELETED
    }

    // ── 메뉴 마스터 관리 ──────────────────────────────────────────────────────

    fun getMenus(sellerId: Long): List<SellerMenuItemResponse> {
        val store = sellerStoreOrNull(sellerId) ?: return emptyList()
        return menuItemRepository.findByStoreAndIsActiveTrue(store).map { SellerMenuItemResponse.from(it) }
    }

    @Transactional
    fun createMenu(sellerId: Long, req: SellerMenuItemRequest, imageUrl: String? = null): SellerMenuItemResponse {
        require(req.name.isNotBlank()) { "메뉴 이름을 입력해 주세요." }
        require(req.originalPrice > 0) { "정상가는 1원 이상이어야 합니다." }
        val menuItem = menuItemRepository.save(
            MenuItem(
                store = sellerStore(sellerId),
                name = req.name,
                description = req.description,
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
        req: SellerMenuItemRequest,
        image: MultipartFile?,
    ): SellerMenuItemResponse {
        val imageUrl = menuImageStorageService.save(image)
        return createMenu(sellerId, req, imageUrl)
    }

    @Transactional
    fun updateMenu(sellerId: Long, menuItemId: Long, req: SellerMenuItemUpdateRequest): SellerMenuItemResponse {
        val store = sellerStore(sellerId)
        val menuItem = findSellerMenu(store, menuItemId)
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
        val menuItem = findSellerMenu(store, menuItemId)
        menuItem.isActive = false
    }

    // ── 사업자 정보 등록 ──────────────────────────────────────────────────────

    @Transactional
    fun registerBusinessInfo(sellerId: Long, businessNumber: String, businessName: String = ""): BusinessInfo {
        val member = memberService.findMember(sellerId)
        require(member.role == MemberRole.SELLER) { "판매자 계정만 사업자 정보를 등록할 수 있습니다." }
        require(businessNumber.isNotBlank()) { "사업자 번호를 입력해 주세요." }
        val info = businessInfoRepository.findByMember(member).orElseGet {
            BusinessInfo(
                member = member,
                businessName = businessName.ifBlank { businessNumber },
                businessNumber = businessNumber,
                isVerified = false,
            )
        }
        info.businessNumber = businessNumber
        if (businessName.isNotBlank()) info.businessName = businessName
        info.isVerified = false
        info.verifiedAt = null
        return businessInfoRepository.save(info)
    }

    // ── 알림 발송 ─────────────────────────────────────────────────────────────

    @Transactional
    fun sendNotification(sellerId: Long, req: SendNotificationRequest): SellerNotificationResponse {
        require(req.message.isNotBlank()) { "알림 내용을 입력해 주세요." }
        val store = sellerStore(sellerId)
        val recipients = notificationRecipients(store, req)
        val saved = recipients.map { member -> saveEventNotification(member, store, req.message) }
        val sent = saved.firstOrNull()
        return SellerNotificationResponse(
            id = sent?.notificationId ?: 0,
            storeId = store.storeId,
            storeName = store.name,
            message = req.message,
            sentAt = (sent?.createdAt ?: now()).toString(),
            recipientCount = recipients.size,
        )
    }

    fun getSellerNotifications(sellerId: Long): List<SellerNotificationResponse> {
        val store = sellerStore(sellerId)
        return notificationRepository.findByRelatedStoreIdAndTitleOrderByCreatedAtDesc(store.storeId, store.name)
            .groupBy { it.body to it.createdAt.toString().take(16) }
            .map { (_, notifications) -> notifications.maxBy { it.createdAt } to notifications.size }
            .sortedByDescending { (first, _) -> first.createdAt }
            .map { (first, recipientCount) ->
                SellerNotificationResponse(
                    id = first.notificationId,
                    storeId = store.storeId,
                    storeName = store.name,
                    message = first.body,
                    sentAt = first.createdAt.toString(),
                    recipientCount = recipientCount,
                )
            }
    }

    private fun notificationRecipients(store: Store, req: SendNotificationRequest): List<com.deuktemsiru.entity.Member> {
        req.radiusKm?.let { require(it > 0) { "알림 반경은 1km 이상이어야 합니다." } }
        val wishlistedMembers = wishlistRepository.findByStore(store)
            .map { it.member }
        val orderMembers = orderRepository.findByStoreOrderByCreatedAtDesc(store)
            .map { it.consumer }
        val base = when (req.targetType.uppercase()) {
            "REGULAR" -> (wishlistedMembers + orderMembers)
            "NEARBY" -> throw IllegalStateException("주변 알림은 회원 위치 정보가 없어 아직 사용할 수 없습니다.")
            else -> throw IllegalArgumentException("지원하지 않는 알림 대상: ${req.targetType}")
        }
        return base
            .filter { it.status }
            .distinctBy { it.memberId }
    }

    private fun findSellerProduct(store: Store, productId: Long): Product =
        findOwnedEntity(
            id = productId,
            notFoundMessage = "판매 상품을 찾을 수 없습니다.",
            findById = productRepository::findByIdOrNull,
            storeIdOf = { it.store.storeId },
            store = store,
        )

    private fun findSellerMenu(store: Store, menuItemId: Long): MenuItem =
        findOwnedEntity(
            id = menuItemId,
            notFoundMessage = "메뉴를 찾을 수 없습니다.",
            findById = menuItemRepository::findByIdOrNull,
            storeIdOf = { it.store.storeId },
            store = store,
        )

    private fun savedImageUrls(images: List<MultipartFile>): List<String> =
        images.mapNotNull { menuImageStorageService.save(it) }

    private fun saveEventNotification(
        member: com.deuktemsiru.entity.Member,
        store: Store,
        message: String,
    ): Notification =
        notificationRepository.save(
            Notification(
                member = member,
                relatedStoreId = store.storeId,
                type = NotificationType.EVENT,
                title = store.name,
                body = message,
            )
        ).also { fcmService.sendToMember(member.memberId, it.title, it.body) }

    private fun <T> findOwnedEntity(
        id: Long,
        notFoundMessage: String,
        findById: (Long) -> T?,
        storeIdOf: (T) -> Long,
        store: Store,
    ): T =
        (findById(id) ?: throw NoSuchElementException(notFoundMessage))
            .also { require(storeIdOf(it) == store.storeId) { "접근 권한이 없습니다." } }

    private fun parsePickupTime(value: String, fieldName: String) = value.toLocalTimeOrThrow(fieldName)

    private fun parseAvailableDate(value: String): LocalDate = value.toLocalDateOrThrow("판매일")

    private fun sellerStore(sellerId: Long) = storeOwnershipService.findSellerStore(sellerId)

    private fun sellerStoreOrNull(sellerId: Long) = storeOwnershipService.findSellerStoreOrNull(sellerId)

    private fun today(): LocalDate = clock.today()

    private fun now(): LocalDateTime = clock.nowDateTime()
}
