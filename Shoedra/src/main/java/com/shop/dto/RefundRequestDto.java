package com.shop.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundRequestDto {
    private Long historyId;   // 환불할 충전내역의 ID
    private String reason;    // 환불 사유
}
