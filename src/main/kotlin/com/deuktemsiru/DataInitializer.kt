package com.deuktemsiru

import com.deuktemsiru.entity.*
import com.deuktemsiru.repository.*
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

private data class MenuData(
    val name: String,
    val emoji: String,
    val originalPrice: Int,
    val discountRate: Int,
    val quantity: Int,
    val pickupTimeSlot: String,
)

private data class StoreData(
    val owner: User,
    val name: String,
    val category: StoreCategory,
    val emoji: String,
    val rating: Float,
    val address: String,
    val phone: String,
    val closingTime: String,
    val menus: List<MenuData>,
)

@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val storeRepository: StoreRepository,
    private val menuItemRepository: MenuItemRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val samplePassword = requireNotNull(passwordEncoder.encode("1234"))
        val sellers = listOf(
            User(email = "bakery@test.com", nickname = "영희네베이커리", passwordHash = samplePassword, role = UserRole.SELLER),
            User(email = "lunchbox@test.com", nickname = "맛있는도시락", passwordHash = samplePassword, role = UserRole.SELLER),
            User(email = "salad@test.com", nickname = "그린샐러드", passwordHash = samplePassword, role = UserRole.SELLER),
            User(email = "cafe1@test.com", nickname = "커피향기", passwordHash = samplePassword, role = UserRole.SELLER),
            User(email = "cafe2@test.com", nickname = "달콤카페", passwordHash = samplePassword, role = UserRole.SELLER),
            User(email = "bakery2@test.com", nickname = "파리크라상", passwordHash = samplePassword, role = UserRole.SELLER),
        ).map { userRepository.save(it) }

        val buyer = userRepository.save(
            User(email = "buyer@test.com", nickname = "홍길동", passwordHash = samplePassword, role = UserRole.BUYER)
        )

        val storeDataList = listOf(
            StoreData(
                owner = sellers[0], name = "영희네 베이커리", category = StoreCategory.BAKERY,
                emoji = "🥐", rating = 4.8f, address = "서울시 마포구 합정동 123-4",
                phone = "02-1234-5678", closingTime = "20:00",
                menus = listOf(
                    MenuData("크루아상 세트", "🥐", 8500, 50, 3, "17:00-18:30"),
                    MenuData("식빵 2개", "🍞", 6000, 40, 5, "17:00-18:30"),
                    MenuData("마카롱 5개", "🍬", 12000, 60, 2, "18:30-20:00"),
                )
            ),
            StoreData(
                owner = sellers[1], name = "맛있는 도시락", category = StoreCategory.LUNCHBOX,
                emoji = "🍱", rating = 4.5f, address = "서울시 강남구 역삼동 456-7",
                phone = "02-2345-6789", closingTime = "19:00",
                menus = listOf(
                    MenuData("닭갈비 도시락", "🍱", 9000, 30, 4, "17:30-19:00"),
                    MenuData("제육볶음 도시락", "🥩", 8500, 40, 3, "17:30-19:00"),
                )
            ),
            StoreData(
                owner = sellers[2], name = "그린 샐러드", category = StoreCategory.SALAD,
                emoji = "🥗", rating = 4.3f, address = "서울시 서초구 방배동 789-1",
                phone = "02-3456-7890", closingTime = "21:00",
                menus = listOf(
                    MenuData("시저 샐러드", "🥗", 11000, 50, 5, "18:00-21:00"),
                    MenuData("그릭 샐러드", "🫙", 10000, 50, 3, "18:00-21:00"),
                    MenuData("두부 샐러드", "🥬", 9500, 30, 2, "19:00-21:00"),
                )
            ),
            StoreData(
                owner = sellers[3], name = "커피향기", category = StoreCategory.CAFE,
                emoji = "☕", rating = 4.6f, address = "서울시 용산구 한남동 321-5",
                phone = "02-4567-8901", closingTime = "22:00",
                menus = listOf(
                    MenuData("아메리카노 2잔", "☕", 9000, 50, 6, "19:00-22:00"),
                    MenuData("케이크 조각", "🍰", 7500, 40, 4, "19:00-22:00"),
                )
            ),
            StoreData(
                owner = sellers[4], name = "달콤카페", category = StoreCategory.CAFE,
                emoji = "🧁", rating = 4.7f, address = "서울시 성동구 성수동 654-3",
                phone = "02-5678-9012", closingTime = "21:30",
                menus = listOf(
                    MenuData("머핀 3개", "🧁", 9000, 60, 5, "18:00-21:30"),
                    MenuData("라떼 2잔", "🥛", 10000, 30, 3, "20:00-21:30"),
                )
            ),
            StoreData(
                owner = sellers[5], name = "파리크라상", category = StoreCategory.BAKERY,
                emoji = "🥖", rating = 4.4f, address = "서울시 마포구 서교동 987-6",
                phone = "02-6789-0123", closingTime = "20:30",
                menus = listOf(
                    MenuData("바게트", "🥖", 5500, 30, 7, "17:00-20:30"),
                    MenuData("소보로빵 4개", "🍞", 8000, 50, 4, "17:00-20:30"),
                )
            ),
        )

        storeDataList.forEach { sd ->
            val store = storeRepository.save(
                Store(
                    owner = sd.owner, name = sd.name, category = sd.category,
                    emoji = sd.emoji, rating = sd.rating, address = sd.address,
                    phone = sd.phone, closingTime = sd.closingTime,
                )
            )
            sd.menus.forEach { md ->
                menuItemRepository.save(
                    MenuItem(
                        store = store, name = md.name, emoji = md.emoji,
                        originalPrice = md.originalPrice,
                        discountedPrice = md.originalPrice * (100 - md.discountRate) / 100,
                        discountRate = md.discountRate,
                        remainingItems = md.quantity,
                        pickupTimeSlot = md.pickupTimeSlot,
                    )
                )
            }
        }

        println("=== 샘플 데이터 초기화 완료 ===")
        println("바이어 계정: buyer@test.com / 1234  (userId: ${buyer.id})")
        sellers.forEachIndexed { i, s -> println("셀러${i + 1}: ${s.email} / 1234  (userId: ${s.id})") }
    }
}
