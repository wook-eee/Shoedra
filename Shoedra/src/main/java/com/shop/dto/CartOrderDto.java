package com.shop.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartOrderDto {
    private Long cartItemId;
    private String impUid;
    private String merchantUid;
    private String payMethod;
    private int amount;

    //private List<CartOrderDto> cartOrderDtoList;
}
