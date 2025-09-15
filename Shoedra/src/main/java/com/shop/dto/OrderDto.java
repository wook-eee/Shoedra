package com.shop.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDto {

    @NotNull(message = "상품 아이디는 필수 입력 값입니다.")
    private Long itemId;

    @Min(value = 1, message = "최소 주문 수량은 1개 입니다.")
    @Max(value = 999, message = "최대 주문 수량은 999개 입니다.")
    private int count;

    private String impUid;        // 포트원 결제 고유 ID
    private String merchantUid;   // 주문번호
    private String payMethod;     // 카드, 계좌이체 등
    private int amount;           // 결제 금액

    // [추가] 포인트(캐시) 사용 금액
    private Integer useCash;

    // [추가] useCash getter/setter
    public Integer getUseCash() {
        return useCash;
    }
    public void setUseCash(Integer useCash) {
        this.useCash = useCash;
    }

    private Long cartItemId;
    public Long getCartItemId() { return cartItemId; }
    public void setCartItemId(Long cartItemId) { this.cartItemId = cartItemId; }
}
