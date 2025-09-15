package com.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuctionBidInfoDto {
    private Long auctionId;
    private String title;
    private int currentPrice;
    private int startingPrice;
    private List<BidDto> bidList;
}
