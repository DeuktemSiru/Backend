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
        // 이미 데이터가 있으면 초기화 건너뜀 (서버 재시작 시 중복 방지)
        if (memberRepository.count() > 0) {
            normalizeExistingSampleData()
            println("=== 이미 샘플 데이터가 존재합니다. 초기화를 건너뜁니다. ===")
            return
        }

        val sellerSeeds = listOf(
            SellerSeed("kakao_seller_1", "bakery@test.com", "김영희", "오이도굽는집"),
            SellerSeed("kakao_seller_2", "cafe@siheung.test", "박민준", "배곧로스터리"),
            SellerSeed("kakao_seller_3", "bunsik@siheung.test", "이수진", "정왕시장분식"),
            SellerSeed("kakao_seller_4", "dosirak@siheung.test", "최하늘", "은행동찬찬도시락"),
            SellerSeed("kakao_seller_5", "mart@siheung.test", "정다은", "목감우리반찬"),
        )
        val sellers = sellerSeeds.map { seed ->
            memberRepository.save(
                Member(
                    provider = MemberProvider.KAKAO,
                    providerId = seed.providerId,
                    email = seed.email,
                    name = seed.name,
                    nickname = seed.nickname,
                    role = MemberRole.SELLER,
                )
            )
        }

        memberRepository.save(
            Member(
                provider = MemberProvider.KAKAO,
                providerId = "kakao_buyer_1",
                email = "buyer@test.com",
                name = "홍길동",
                nickname = "시흥득템러",
                role = MemberRole.CONSUMER,
            )
        )

        val storeInfos = listOf(
            StoreSeed(
                owner = sellers[0],
                name = "오이도굽는집",
                category = CategoryType.BAKERY,
                description = "오이도 산책로 근처에서 당일 생산한 빵을 저녁 픽업으로 판매합니다.",
                address = "경기도 시흥시 오이도로 135 1층",
                latitude = 37.3452,
                longitude = 126.6878,
                phone = "031-498-1350",
                ratingAvg = 4.8,
                reviewCount = 126,
                menuName = "소금빵 4개입",
                menuDescription = "버터 향이 진한 당일 생산 소금빵 묶음",
                originalPrice = 12000,
                discountPrice = 7200,
                quantityTotal = 8,
                quantityRemaining = 5,
                pickupStart = LocalTime.of(18, 30),
                pickupEnd = LocalTime.of(20, 30),
                carbonSavedKg = 0.42,
                allergenInfo = "밀, 우유, 대두",
            ),
            StoreSeed(
                owner = sellers[1],
                name = "배곧 로스터리",
                category = CategoryType.CAFE,
                description = "배곧신도시 카페거리의 로컬 로스터리입니다.",
                address = "경기도 시흥시 서울대학로278번길 25-8 102호",
                latitude = 37.3689,
                longitude = 126.7296,
                phone = "031-431-2788",
                ratingAvg = 4.7,
                reviewCount = 89,
                menuName = "디저트 랜덤박스",
                menuDescription = "휘낭시에, 스콘, 쿠키 중 당일 남은 디저트 구성",
                originalPrice = 15000,
                discountPrice = 9000,
                quantityTotal = 6,
                quantityRemaining = 3,
                pickupStart = LocalTime.of(19, 0),
                pickupEnd = LocalTime.of(21, 0),
                carbonSavedKg = 0.36,
                allergenInfo = "밀, 우유, 달걀",
            ),
            StoreSeed(
                owner = sellers[2],
                name = "정왕시장 분식",
                category = CategoryType.RESTAURANT,
                description = "정왕시장 골목에서 매일 준비하는 분식과 간편식을 할인 판매합니다.",
                address = "경기도 시흥시 정왕시장길 52 1층",
                latitude = 37.3501,
                longitude = 126.7427,
                phone = "031-499-5200",
                ratingAvg = 4.6,
                reviewCount = 214,
                menuName = "떡볶이 순대 세트",
                menuDescription = "매콤 떡볶이 1인분과 찰순대 소포장",
                originalPrice = 11000,
                discountPrice = 6500,
                quantityTotal = 10,
                quantityRemaining = 7,
                pickupStart = LocalTime.of(17, 30),
                pickupEnd = LocalTime.of(19, 30),
                carbonSavedKg = 0.51,
                allergenInfo = "밀, 대두, 돼지고기",
            ),
            StoreSeed(
                owner = sellers[3],
                name = "은행동 찬찬도시락",
                category = CategoryType.RESTAURANT,
                description = "은행단지와 대야역 생활권 직장인을 위한 집밥 도시락 매장입니다.",
                address = "경기도 시흥시 은행로 149 1층",
                latitude = 37.4411,
                longitude = 126.8026,
                phone = "031-313-1490",
                ratingAvg = 4.9,
                reviewCount = 73,
                menuName = "제육 도시락",
                menuDescription = "제육볶음, 계절 반찬, 잡곡밥으로 구성된 도시락",
                originalPrice = 9500,
                discountPrice = 5900,
                quantityTotal = 7,
                quantityRemaining = 4,
                pickupStart = LocalTime.of(18, 0),
                pickupEnd = LocalTime.of(20, 0),
                carbonSavedKg = 0.47,
                allergenInfo = "대두, 돼지고기, 참깨",
            ),
            StoreSeed(
                owner = sellers[4],
                name = "목감 우리반찬",
                category = CategoryType.GROCERY,
                description = "목감지구 아파트 단지 앞 반찬가게입니다.",
                address = "경기도 시흥시 목감둘레로 253 103호",
                latitude = 37.3843,
                longitude = 126.8596,
                phone = "031-411-2530",
                ratingAvg = 4.7,
                reviewCount = 58,
                menuName = "오늘의 반찬 3종",
                menuDescription = "나물, 볶음, 조림류 중 당일 준비한 반찬 3팩",
                originalPrice = 13500,
                discountPrice = 7900,
                quantityTotal = 9,
                quantityRemaining = 6,
                pickupStart = LocalTime.of(18, 30),
                pickupEnd = LocalTime.of(20, 30),
                carbonSavedKg = 0.55,
                allergenInfo = "대두, 참깨",
            ),
        )

        storeInfos.forEach { seed ->
            val store = storeRepository.save(
                Store(
                    owner = seed.owner,
                    name = seed.name,
                    description = seed.description,
                    address = seed.address,
                    latitude = seed.latitude,
                    longitude = seed.longitude,
                    phone = seed.phone,
                    ratingAvg = seed.ratingAvg,
                    reviewCount = seed.reviewCount,
                    isVerified = true,
                )
            )
            storeCategoryRepository.save(StoreCategory(store = store, category = seed.category))

            val menuItem = menuItemRepository.save(
                MenuItem(
                    store = store,
                    name = seed.menuName,
                    description = seed.menuDescription,
                    originalPrice = seed.originalPrice,
                    allergenInfo = seed.allergenInfo,
                )
            )
            productRepository.save(
                Product(
                    store = store,
                    menuItem = menuItem,
                    name = "오늘 마감할인 ${menuItem.name}",
                    description = seed.menuDescription,
                    originalPrice = seed.originalPrice,
                    discountPrice = seed.discountPrice,
                    quantityTotal = seed.quantityTotal,
                    quantityRemaining = seed.quantityRemaining,
                    allergenInfo = seed.allergenInfo,
                    madeAt = "오늘 오전 제조",
                    pickupStart = seed.pickupStart,
                    pickupEnd = seed.pickupEnd,
                    availableDate = LocalDate.now(),
                    carbonSavedKg = seed.carbonSavedKg,
                )
            )
        }

        println("=== 샘플 데이터 초기화 완료 ===")
    }

    private fun normalizeExistingSampleData() {
        memberRepository.findByEmail("bakery@test.com").ifPresent { seller ->
            storeRepository.findByOwner(seller).ifPresent { store ->
                if (store.name == "오이도 굽는집") {
                    store.name = "오이도굽는집"
                    storeRepository.save(store)
                }
            }
        }
    }

    private data class SellerSeed(
        val providerId: String,
        val email: String,
        val name: String,
        val nickname: String,
    )

    private data class StoreSeed(
        val owner: Member,
        val name: String,
        val category: CategoryType,
        val description: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val phone: String,
        val ratingAvg: Double,
        val reviewCount: Int,
        val menuName: String,
        val menuDescription: String,
        val originalPrice: Int,
        val discountPrice: Int,
        val quantityTotal: Int,
        val quantityRemaining: Int,
        val pickupStart: LocalTime,
        val pickupEnd: LocalTime,
        val carbonSavedKg: Double,
        val allergenInfo: String,
    )
}
