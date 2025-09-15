package com.shop.constant;

public enum BidStatus {
    ACTIVE,     // 현재 유효한 입찰
    CANCELLED,  // 사용자가 직접 취소
    OUTBID,     // 더 높은 입찰가에 밀림
    WINNER,     // 낙찰자
    PAID,       // 결제완료
    EXPIRED     // 경매 종료 후 자동 만료?
}
