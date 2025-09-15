package com.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CashHistoryResponseDto {
    private Long id;
    private String memberName;
    private int amount;
    private String status;

//    private String orderId; // ✅ 이게 없으면 에러 발생
}