package com.deuktemsiru.service

import com.deuktemsiru.dto.*
import com.deuktemsiru.entity.BusinessInfo
import com.deuktemsiru.entity.Member
import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.MenuItem
import com.deuktemsiru.entity.Notification
import com.deuktemsiru.entity.NotificationType
import com.deuktemsiru.entity.OrderStatus
import com.deuktemsiru.entity.Product
import com.deuktemsiru.entity.ProductImage
import com.deuktemsiru.entity.ProductStatus
import com.deuktemsiru.entity.Store
import com.deuktemsiru.entity.changeSaleStatus
import com.deuktemsiru.repository.BusinessInfoRepository
import com.deuktemsiru.repository.MenuItemRepository
import com.deuktemsiru.repository.NotificationRepository
import com.deuktemsiru.repository.OrderRepository
import com.deuktemsiru.repository.ProductRepository
import com.deuktemsiru.repository.WishlistRepository
import com.deuktemsiru.common.orNotFound
import com.deuktemsiru.common.toLocalDateOrThrow
import com.deuktemsiru.common.toLocalTimeOrThrow
import com.deuktemsiru.common.nowDateTime
import com.deuktemsiru.common.today
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
        val store = storeOwnershipService.findSellerStore(sellerId)
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
        val store = storeOwnershipService.findSellerStore(sellerId)
        req.description?.let { store.description = it }
        req.address?.takeIf { it.isNotBlank() }?.let { store.address = it }
        req.phone?.let { store.phone = it }
        req.closingTime?.takeIf { it.isNotBlank() }?.let {
            it.toLocalTimeOrThrow("마감 시간")
            store.closingTime = it
        }
        return getStore(sellerId)
    }

    // ── 상품 관리 ─────────────────────────────────────────────────────────────

    fun getProducts(sellerId: Long, date: LocalDate? = null, status: String? = null): List<SellerSaleItemResponse> {
        val store = storeOwnershipService.findSellerStoreOrNull(sellerId) ?: return emptyList()
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
    fun createProduct(
        sellerId: Long,
        req: SaleItemRequest,
        images: List<MultipartFile> = emptyList(),
    ): SellerSaleItemResponse {
        require(req.name.isNotBlank()) { "상품 이름을 입력해 주세요." }
        require(req.quantityTotal > 0) { "판매 수량은 1개 이상이어야 합니다." }
        validatePrice(req.originalPrice, req.discountPrice)

        val store = storeOwnershipService.findSellerStore(sellerId)
        val menuItem = req.menuItemId?.let { findSellerMenu(store, it) }

        val pickupStart = req.pickupStart.toLocalTimeOrThrow("픽업 시작 시간")
        val pickupEnd = req.pickupEnd.toLocalTimeOrThrow("픽업 종료 시간")
        require(pickupEnd.isAfter(pickupStart)) { "픽업 종료 시간은 시작 시간보다 늦어야 합니다." }

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
                availableDate = req.availableDate.toLocalDateOrThrow("판매일"),
            )
        )
        product.addImages(images)
        return SellerSaleItemResponse.from(product)
    }

    @Transactional
    fun updateProductStatus(sellerId: Long, productId: Long, req: UpdateSaleStatusRequest): SellerSaleItemResponse {
        val store = storeOwnershipService.findSellerStore(sellerId)
        val product = findSellerProduct(store, productId)
        product.changeSaleStatus(req.status.toProductStatus())
        return SellerSaleItemResponse.from(product)
    }

    @Transactional
    fun updateProduct(sellerId: Long, productId: Long, req: UpdateSaleItemRequest): SellerSaleItemResponse {
        val store = storeOwnershipService.findSellerStore(sellerId)
        val product = findSellerProduct(store, productId)
        require(product.status != ProductStatus.DELETED) { "삭제된 상품은 수정할 수 없습니다." }

        val nextOriginalPrice = req.originalPrice ?: product.originalPrice
        val nextDiscountPrice = req.discountPrice ?: product.discountPrice
        validatePrice(nextOriginalPrice, nextDiscountPrice)
        product.originalPrice = nextOriginalPrice
        product.discountPrice = nextDiscountPrice

        req.quantityRemaining?.let {
            require(it >= 0) { "잔여 수량은 0개 이상이어야 합니다." }
            require(it <= product.quantityTotal) { "잔여 수량은 총 수량보다 많을 수 없습니다." }
            product.quantityRemaining = it
            product.status = when {
                it == 0 -> ProductStatus.SOLD_OUT
                product.status == ProductStatus.SOLD_OUT -> ProductStatus.AVAILABLE
                else -> product.status
            }
        }
        return SellerSaleItemResponse.from(product)
    }

    @Transactional
    fun deleteProduct(sellerId: Long, productId: Long) {
        val store = storeOwnershipService.findSellerStore(sellerId)
        val product = findSellerProduct(store, productId)
        val activeStatuses = listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)
        require(!orderRepository.existsActiveOrderForProduct(store, activeStatuses, product)) {
            "진행 중인 주문이 있는 상품은 취소할 수 없습니다."
        }
        product.status = ProductStatus.DELETED
    }

    // ── 메뉴 마스터 관리 ──────────────────────────────────────────────────────

    fun getMenus(sellerId: Long): List<SellerMenuItemResponse> {
        val store = storeOwnershipService.findSellerStoreOrNull(sellerId) ?: return emptyList()
        return menuItemRepository.findByStoreAndIsActiveTrue(store).map { SellerMenuItemResponse.from(it) }
    }

    @Transactional
    fun createMenu(sellerId: Long, req: SellerMenuItemRequest, imageUrl: String? = null): SellerMenuItemResponse {
        require(req.name.isNotBlank()) { "메뉴 이름을 입력해 주세요." }
        require(req.originalPrice > 0) { "정상가는 1원 이상이어야 합니다." }
        val menuItem = menuItemRepository.save(
            MenuItem(
                store = storeOwnershipService.findSellerStore(sellerId),
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
        return createMenu(sellerId, req, menuImageStorageService.save(image))
    }

    @Transactional
    fun updateMenu(sellerId: Long, menuItemId: Long, req: SellerMenuItemUpdateRequest): SellerMenuItemResponse {
        val store = storeOwnershipService.findSellerStore(sellerId)
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
        val store = storeOwnershipService.findSellerStore(sellerId)
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
        val store = storeOwnershipService.findSellerStore(sellerId)
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
        val store = storeOwnershipService.findSellerStore(sellerId)
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

    private fun notificationRecipients(store: Store, req: SendNotificationRequest): List<Member> {
        req.radiusKm?.let { require(it > 0) { "알림 반경은 1km 이상이어야 합니다." } }
        val base = when (req.targetType.uppercase()) {
            "REGULAR" -> sequenceOf(
                wishlistRepository.findByStore(store).asSequence().map { it.member },
                orderRepository.findByStoreOrderByCreatedAtDesc(store).asSequence().map { it.consumer },
            ).flatten()
            "NEARBY" -> throw IllegalStateException("주변 알림은 회원 위치 정보가 없어 아직 사용할 수 없습니다.")
            else -> throw IllegalArgumentException("지원하지 않는 알림 대상: ${req.targetType}")
        }
        return base
            .filter { it.status }
            .distinctBy { it.memberId }
            .toList()
    }

    private fun findSellerProduct(store: Store, productId: Long): Product =
        productRepository.findById(productId).orNotFound("판매 상품을 찾을 수 없습니다.")
            .also { require(it.store.storeId == store.storeId) { "접근 권한이 없습니다." } }

    private fun findSellerMenu(store: Store, menuItemId: Long): MenuItem =
        menuItemRepository.findById(menuItemId).orNotFound("메뉴를 찾을 수 없습니다.")
            .also { require(it.store.storeId == store.storeId) { "접근 권한이 없습니다." } }

    private fun Product.addImages(images: List<MultipartFile>) {
        images.mapNotNull { menuImageStorageService.save(it) }.forEachIndexed { index, imageUrl ->
            this.images += ProductImage(product = this, imageUrl = imageUrl, displayOrder = index)
        }
        thumbnailUrl = this.images.minByOrNull { it.displayOrder }?.imageUrl ?: thumbnailUrl
    }

    private fun validatePrice(originalPrice: Int, discountPrice: Int) {
        require(originalPrice > 0) { "정상가는 1원 이상이어야 합니다." }
        require(discountPrice > 0) { "할인가는 1원 이상이어야 합니다." }
        require(discountPrice < originalPrice) { "할인가는 정가보다 낮아야 합니다." }
    }

    private fun saveEventNotification(member: Member, store: Store, message: String): Notification =
        notificationRepository.save(
            Notification(
                member = member,
                relatedStoreId = store.storeId,
                type = NotificationType.EVENT,
                title = store.name,
                body = message,
            )
        ).also { fcmService.sendToMember(member.memberId, it.title, it.body) }

    private fun today(): LocalDate = clock.today()

    private fun now(): LocalDateTime = clock.nowDateTime()
}
