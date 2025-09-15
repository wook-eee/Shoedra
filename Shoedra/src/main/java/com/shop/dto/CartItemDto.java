package com.shop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemDto {

    @NotNull(message = "상품 아이디는 필수 입력 값 입니다.")
    private Long itemId;

    @Min(value = 1, message = "최소 1개 이상 담아주세요. ")
    private int count;
    
    // ✅ 사이즈 정보 추가 (선택사항)
    private Integer size;
    
    /**
     * ✅ 사이즈 유효성 검증
     * @return 유효한 사이즈인지 확인
     */
    public boolean isValidSize() {
        if (size == null) return true; // 사이즈가 없어도 유효 (기본 상품)
        
        // 신발 사이즈 범위: 220 ~ 280 (5단위)
        return size >= 220 && size <= 280 && size % 5 == 0;
    }
    
    /**
     * ✅ 사이즈 표시 문자열
     */
    public String getSizeDisplay() {
        return size != null ? size + "mm" : "사이즈 없음";
    }
}
