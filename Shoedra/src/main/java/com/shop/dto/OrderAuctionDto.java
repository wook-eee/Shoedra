package com.shop.dto;

import com.shop.entity.OrderAuction;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderAuctionDto {
    private String itemNm;
    //private int count;
    private int orderPrice;
    private String imgUrl;

    public OrderAuctionDto(OrderAuction orderAuction, String imgUrl){
        this.itemNm = orderAuction.getAuction().getItem().getItemNm();
        //this.count = orderAuction.
        this.orderPrice = orderAuction.getAuction().getCurrentBidPrice();
        this.imgUrl = imgUrl;
    }
}
