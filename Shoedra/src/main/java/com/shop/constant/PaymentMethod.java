package com.shop.constant;

public enum PaymentMethod {
    CARD,       // 일반 카드결제
    KAKAO,      // 카카오페이
    PORTONE,    // 포트원
    ADMIN,      // 관리자 수동충전
    CHARGE,     // 충전
    INTERNAL,   // 내부 소비
    REFUND      // 환불 처리
}
