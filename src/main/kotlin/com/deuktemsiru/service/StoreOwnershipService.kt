package com.deuktemsiru.service

import com.deuktemsiru.entity.MemberRole
import com.deuktemsiru.entity.Store
import com.deuktemsiru.repository.StoreRepository
import org.springframework.stereotype.Service

@Service
class StoreOwnershipService(
    private val memberService: MemberService,
    private val storeRepository: StoreRepository,
) {
    fun findSellerStore(sellerId: Long): Store {
        val seller = memberService.findMember(sellerId)
        require(seller.role == MemberRole.SELLER) { "판매자 계정만 처리할 수 있습니다." }
        return storeRepository.findByOwner(seller)
            .orElseThrow { NoSuchElementException("등록된 가게가 없습니다.") }
    }

    fun findSellerStoreOrNull(sellerId: Long): Store? {
        val seller = memberService.findMember(sellerId)
        require(seller.role == MemberRole.SELLER) { "판매자 계정만 처리할 수 있습니다." }
        return storeRepository.findByOwner(seller).orElse(null)
    }
}
