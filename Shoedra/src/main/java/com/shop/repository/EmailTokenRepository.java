package com.shop.repository;

import com.shop.entity.EmailToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface EmailTokenRepository extends JpaRepository<EmailToken, Integer> {
    EmailToken findByToken(String token);

    EmailToken findByEmail(String email);

    // 만료 기간이 지난 토큰 삭제
    @Transactional
    void deleteByExpiryDateBefore(LocalDateTime now);

    // 인증 상태를 1로 업데이트
    @Modifying
    @Transactional
    @Query("UPDATE EmailToken e SET e.verified = true WHERE e.token = :token")
    void updateVerifiedByToken(String token);

    // 해당 이메일의 토큰 삭제
    @Transactional
    void deleteByEmail(String email);
}
