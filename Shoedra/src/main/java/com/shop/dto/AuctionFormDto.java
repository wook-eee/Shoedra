package com.shop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AuctionFormDto {
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long itemId;

    @Min(value = 1000, message = "시작가는 최소 1,000원 이상이어야 합니다.")
    private int startPrice;

    @NotNull(message = "경매 시작 시간을 입력해주세요.")
    private LocalDateTime startTime;

    @NotNull(message = "경매 종료 시간을 입력해주세요.")
    private LocalDateTime endTime;
}
