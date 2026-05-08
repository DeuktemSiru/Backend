package com.deuktemsiru

import com.deuktemsiru.entity.*
import com.deuktemsiru.repository.*
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime

@Component
class DataInitializer(
    private val memberRepository: MemberRepository,
    private val storeRepository: StoreRepository,
    private val storeCategoryRepository: StoreCategoryRepository,
    private val menuItemRepository: MenuItemRepository,
    private val productRepository: ProductRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val sellers = listOf(
            Member(provider = MemberProvider.KAKAO, providerId = "kakao_seller_1", email = "bakery@test.com", name = "영희", nickname = "영희네베이커리", role = MemberRole.SELLER),
            Member(provider = MemberProvider.KAKAO, providerId = "kakao_seller_2", email = "cafe@test.com",   name = "민준", nickname = "커피향기",       role = MemberRole.SELLER),
            Member(provider = MemberProvider.KAKAO, providerId = "kakao_seller_3", email = "resto@test.com",  name = "수진", nickname = "맛있는식당",      role = MemberRole.SELLER),
        ).map { memberRepository.save(it) }

        memberRepository.save(
            Member(provider = MemberProvider.KAKAO, providerId = "kakao_buyer_1", email = "buyer@test.com", name = "홍길동", nickname = "득템러", role = MemberRole.CONSUMER)
        )

        val storeInfos = listOf(
            Triple(sellers[0], "영희네 베이커리",  CategoryType.BAKERY),
            Triple(sellers[1], "커피향기",         CategoryType.CAFE),
            Triple(sellers[2], "맛있는식당",        CategoryType.RESTAURANT),
        )

        storeInfos.forEach { (seller, storeName, category) ->
            val store = storeRepository.save(
                Store(
                    owner = seller, name = storeName,
                    address = "서울시 마포구 합정동 123-4",
                    latitude = 37.5499, longitude = 126.9145,
                    isVerified = true,
                )
            )
            storeCategoryRepository.save(StoreCategory(store = store, category = category))

            val menuItem = menuItemRepository.save(
                MenuItem(store = store, name = "샘플 메뉴", originalPrice = 8000)
            )
            productRepository.save(
                Product(
                    store = store,
                    menuItem = menuItem,
                    name = "마감할인 ${menuItem.name}",
                    originalPrice = 8000,
                    discountPrice = 4000,
                    quantityTotal = 5,
                    quantityRemaining = 5,
                    pickupStart = LocalTime.of(17, 0),
                    pickupEnd = LocalTime.of(20, 0),
                    availableDate = LocalDate.now(),
                    carbonSavedKg = 0.3,
                )
            )
        }

        println("=== 샘플 데이터 초기화 완료 ===")
    }
}
