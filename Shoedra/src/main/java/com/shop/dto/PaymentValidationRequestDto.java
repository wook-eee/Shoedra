package com.shop.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentValidationRequestDto {
    private String impUid;     // 아임포트 결제 고유 UID
    private int amount;
    private String merchant_uid;// 결제 금액
    private String payMethod; // 추가
    private String method;
    private String merchantUid;
}
