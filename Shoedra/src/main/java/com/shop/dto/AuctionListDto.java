package com.shop.dto;

import com.shop.constant.AuctionState;
import com.shop.entity.Auction;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AuctionListDto {
    private Long id;
    private String itemName;
    private int startPrice;
    private int currentBidPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionState auctionState;
    private String repImgUrl; // 대표 이미지 URL 추가

    public AuctionListDto(Auction auction, String repImgUrl) {
        this.id = auction.getId();
        this.itemName = auction.getItem().getItemNm();
        this.startPrice = auction.getStartPrice();
        this.currentBidPrice = auction.getCurrentBidPrice();
        this.startTime = auction.getStartTime();
        this.endTime = auction.getEndTime();
        this.auctionState = auction.getAuctionState();
        this.repImgUrl = repImgUrl;
    }
}
