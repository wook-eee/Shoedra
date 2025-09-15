package com.shop.repository;


import com.shop.entity.CashHistory;
import com.shop.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CashHistoryRepository extends JpaRepository<CashHistory, Long> {
//    List<CashHistory> findByMemberIdOrderByChargedAtDesc(Long memberId);
//    List<CashHistory> findByMemberIdAndChargedAtAfterOrderByChargedAtDesc(Long memberId, LocalDateTime from);

    // 특정 회원의 캐시 충전/이력 전체 조회 (최신순)
    List<CashHistory> findByMemberOrderByCreatedAtDesc(Member member);

    // 또는 memberId만 사용하는 경우
    List<CashHistory> findByMember_IdOrderByCreatedAtDesc(Long memberId);
}
