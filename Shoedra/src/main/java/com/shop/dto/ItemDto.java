package com.shop.dto;

import com.shop.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ItemDto {
//    private Long id;
//    private String itemNm;
//    private Integer price;
//    private String itemDetail;
//    private String sellStatCd;
//    private LocalDateTime regTime;
//    private LocalDateTime updateTime;
        private Long id;
        private String itemNm;
        private int price;
        private String repImgUrl;
        private String itemState;
        private String auctionState;


        public ItemDto(Long id, String itemNm, int price, String repImgUrl, String auctionState, String itemState) {
            this.id = id;
            this.itemNm = itemNm;
            this.price = price;
            this.repImgUrl = repImgUrl;
            this.auctionState = auctionState;
            this.itemState = itemState;
    }
    public ItemDto(Item item) {
        this.id = item.getId();
        this.itemNm = item.getItemNm();
        this.price = item.getPrice();
        this.repImgUrl = item.getRepImgUrl(); // 세션 안에서 안전하게 호출 가능
    }
}

