package com.shop.dto;

import com.shop.constant.ItemSellStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ItemToSellDto {
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long dataItemId;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
    private Integer price;

    private String color;

    // 재고는 사이즈별 재고 처리 후에 설정되므로 검증 제거
    private Integer stockNumber;

    private String title;

    private ItemSellStatus itemSellStatus;
    
    // 사이즈별 재고 저장
    private Map<String, Integer> sizeStock = new HashMap<>();
}
