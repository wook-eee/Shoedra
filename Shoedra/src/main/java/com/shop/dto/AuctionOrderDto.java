package com.shop.dto;

import com.shop.entity.Auction;
import com.shop.entity.Item;
import com.shop.entity.ItemImg;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuctionOrderDto {
    private Long auctionId;
    private String itemName;
    private String repImgUrl;
    private int winningPrice;

    public AuctionOrderDto(Auction auction) {
        this.auctionId = auction.getId();

        Item item = auction.getItem();
        this.itemName = item.getItemNm();

        this.repImgUrl = item.getItemImgs().stream()
                .filter(img -> "Y".equals(img.getRepImgYn()))
                .map(ItemImg::getImgUrl)
                .findFirst()
                .orElse("/images/default.png");

        this.winningPrice = auction.getCurrentBidPrice();
    }
}
