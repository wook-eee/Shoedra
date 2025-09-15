package com.shop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BidRequestDto {
    private Long memberId;

    @NotNull(message = "경매 ID는 필수입니다.")
    private Long auctionId;

    @Min(value = 1000, message = "입찰가는 최소 1000원 이상이어야 합니다.")
    private int bidPrice;
}
