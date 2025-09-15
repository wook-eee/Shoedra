package com.shop.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CartDetailDto {
    private Long cartItemId; // 장바구니 상품 아이디
    private String itemNm; // 상품명
    private int price; // 가격
    private int count; // 수량
    private String imgUrl;
    private Long itemId;
    private Integer size; // ✅ 사이즈 정보 추가

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    /**
     * ✅ 기본 생성자 (사이즈 정보 없음)
     */
    public CartDetailDto(Long itemId, Long cartItemId, String itemNm, int price, int count, String imgUrl){
        this.itemId = itemId;
        this.cartItemId = cartItemId;
        this.itemNm = itemNm;
        this.price = price;
        this.count = count;
        this.imgUrl = imgUrl;
        this.size = null;
    }
    
    /**
     * ✅ 사이즈 정보 포함 생성자
     */
    public CartDetailDto(Long itemId, Long cartItemId, String itemNm, int price, int count, String imgUrl, Integer size){
        this.itemId = itemId;
        this.cartItemId = cartItemId;
        this.itemNm = itemNm;
        this.price = price;
        this.count = count;
        this.imgUrl = imgUrl;
        this.size = size;
    }
    
    /**
     * ✅ 사이즈 표시 문자열
     */
    public String getSizeDisplay() {
        return size != null ? size + "mm" : "사이즈 없음";
    }
    
    /**
     * ✅ 총 가격 계산
     */
    public int getTotalPrice() {
        return price * count;
    }
}
