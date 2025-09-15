package com.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor

public class BidHistoryDto {
    private Long auctionId;
    private String itemName;
    private int bidPrice;
    private LocalDateTime bidTime;
    private boolean isWinning;

    // 생성자
    public BidHistoryDto(Long auctionId,String itemName, int bidPrice, LocalDateTime bidTime, boolean isWinning) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.bidPrice = bidPrice;
        this.bidTime = bidTime;
        this.isWinning = isWinning;
    }
}
