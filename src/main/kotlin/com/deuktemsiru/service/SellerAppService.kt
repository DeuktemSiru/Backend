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
import com.deuktemsiru.repository.WishlistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

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
    // S3: sellerStore() 로직을 OrderService 에서 공유하여 중복 제거
    private val orderService: OrderService,
) {

    // ── 가게 조회/수정 ────────────────────────────────────────────────────────

    fun getStore(sellerId: Long): SellerStoreResponse {
        val store = sellerStore(sellerId)
        val today = LocalDate.now()
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
        val store = sellerStore(sellerId)
        val targetDate = date ?: LocalDate.now()
        val products = if (status != null) {
            val statusEnum = parseProductStatus(status)
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
        require(req.quantityTotal > 0) { "판매 수량은 1개 이상이어야 합니다." }
        require(req.discountPrice > 0) { "할인가는 1원 이상이어야 합니다." }
        require(req.discountPrice < req.originalPrice) { "할인가는 정가보다 낮아야 합니다." }

        val store = sellerStore(sellerId)
        val menuItem = req.menuItemId?.let {
            menuItemRepository.findById(it)
                .orElseThrow { NoSuchElementException("메뉴를 찾을 수 없습니다.") }
                .also { mi -> require(mi.store.storeId == store.storeId) { "내 매장의 메뉴만 등록할 수 있습니다." } }
        }

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
        val nextStatus = parseProductStatus(req.status)
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
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("판매 상품을 찾을 수 없습니다.") }
        require(product.store.storeId == store.storeId) { "접근 권한이 없습니다." }

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
        val product = productRepository.findById(productId)
            .orElseThrow { NoSuchElementException("판매 상품을 찾을 수 없습니다.") }
        require(product.store.storeId == store.storeId) { "접근 권한이 없습니다." }
        val activeStatuses = setOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)
        val hasActiveOrders = orderRepository.findByStoreOrderByCreatedAtDesc(store)
            .any { order -> order.status in activeStatuses && order.items.any { it.product.productId == product.productId } }
        require(!hasActiveOrders) { "진행 중인 주문이 있는 상품은 취소할 수 없습니다." }
        product.status = ProductStatus.DELETED
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
    fun registerBusinessInfo(sellerId: Long, businessNumber: String, businessName: String = ""): BusinessInfo {
        val member = memberService.findMember(sellerId)
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
        return notificationRepository.findByRelatedStoreIdAndTitleOrderByCreatedAtDesc(store.storeId, store.name)
            .groupBy { it.body to it.createdAt.toString().take(16) }
            .map { (_, notifications) ->
                val first = notifications.maxBy { it.createdAt }
                first.createdAt to SellerNotificationResponse(
                    id = first.notificationId,
                    storeId = store.storeId,
                    storeName = store.name,
                    message = first.body,
                    sentAt = first.createdAt.toString(),
                    recipientCount = notifications.size,
                )
            }
            .sortedByDescending { (createdAt, _) -> createdAt }
            .map { (_, response) -> response }
    }

    private fun notificationRecipients(store: Store, req: SendNotificationRequest): List<com.deuktemsiru.entity.Member> {
        val wishlistedMembers = wishlistRepository.findByStore(store)
            .map { it.member }
        val orderMembers = orderRepository.findByStoreOrderByCreatedAtDesc(store)
            .map { it.consumer }
        val base = when (req.targetType.uppercase()) {
            "REGULAR", "NEARBY" -> (wishlistedMembers + orderMembers)
            else -> throw IllegalArgumentException("지원하지 않는 알림 대상: ${req.targetType}")
        }
        return base
            .filter { it.status }
            .distinctBy { it.memberId }
    }

    private fun parsePickupTime(value: String, fieldName: String): LocalTime =
        runCatching { LocalTime.parse(value) }
            .getOrElse { throw IllegalArgumentException("$fieldName 형식은 HH:mm 이어야 합니다.") }

    private fun parseAvailableDate(value: String): LocalDate =
        runCatching { LocalDate.parse(value) }
            .getOrElse { throw IllegalArgumentException("판매일 형식은 yyyy-MM-dd 이어야 합니다.") }

    // S3: OrderService.sellerStore() 에 위임하여 중복 제거
    private fun sellerStore(sellerId: Long) = orderService.sellerStore(sellerId)
}
