package com.shop.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.constant.ItemSellStatus;
import com.shop.entity.Item;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ItemFormDto {
    // Item
    private Long id;

    @NotBlank(message = "상품명은 필수 입력 값입니다.")
    private String itemNm;

    @NotNull(message = "가격은 필수 입력 값입니다.")
    private Integer price;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String itemDetail;

    @NotNull(message = "재고는 필수 입력 값입니다.")
    private Integer stockNumber;

    private ItemSellStatus itemSellStatus;
    
    // 사이즈별 재고 저장
    private Map<String, Integer> sizeStock = new HashMap<>();
    
    //-------------------------------------------
    // ItemImg
    private List<ItemImgDto> itemImgDtoList = new ArrayList<>(); // 상품 이미지 정보

    private List<Long> itemImgIds = new ArrayList<>(); // 상품 이미지 아이디

    //---------------------------------------------------------------------
    // ModelMapper
    private static ModelMapper modelMapper = new ModelMapper();
    private static ObjectMapper objectMapper = new ObjectMapper();

    public Item createItem(){
        //ItemFormDto -> Item 연결 DTO -> Entity
        return modelMapper.map(this, Item.class);
    }

    public static ItemFormDto of(Item item){
        // Entity -> DTO
        // Item -> ItemFormDto 연결
        ItemFormDto itemFormDto = modelMapper.map(item, ItemFormDto.class);
        
        // 사이즈별 재고 정보 파싱
        if (item.getSizeStockJson() != null && !item.getSizeStockJson().trim().isEmpty()) {
            try {
                Map<String, Integer> sizeStockMap = objectMapper.readValue(
                    item.getSizeStockJson(), 
                    new TypeReference<Map<String, Integer>>() {}
                );
                itemFormDto.setSizeStock(sizeStockMap);
            } catch (JsonProcessingException e) {
                System.err.println("사이즈별 재고 JSON 파싱 실패: " + e.getMessage());
            }
        }
        
        return itemFormDto;
    }
}
