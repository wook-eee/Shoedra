package com.shop.entity;

import com.shop.constant.ItemSellStatus;
import com.shop.dto.ItemFormDto;
import com.shop.dto.ItemToSellDto;
import com.shop.exception.OutOfStockException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@Table(name = "item")
@Getter
@Setter
@ToString
public class Item extends BaseEntity{
    @Id
    @Column(name = "item_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id; // 상품코드

    @Column(nullable = false,length = 50)
    private String itemNm; // 상품명

    @Column(name = "price", nullable = false)
    private int price; // 가격

    @Column(nullable = false)
    private int stockNumber; // 수량

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String itemDetail; // 상품상세설명

    @Enumerated(EnumType.STRING)
    private ItemSellStatus itemSellStatus; // 상품판매 상태

    // 사이즈별 재고 정보 (JSON 형태로 저장)
    @Column(columnDefinition = "TEXT")
    private String sizeStockJson;

   
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemImg> itemImgs = new ArrayList<>();



    @Transient  // DB 컬럼이 아님을 명시
    public String getRepImgUrl() {
        return this.itemImgs.stream()
                .filter(img -> "Y".equals(img.getRepImgYn()))
                .findFirst()
                .map(ItemImg::getImgUrl) // 또는 getImagePath 등
                .orElse("/img/default.png"); // 대표 이미지 없을 때 기본 이미지
    }


    /*private LocalDateTime regTime; // 등록 시간
    private LocalDateTime updateTime; // 수정 시간*/

    public void updateItem(ItemFormDto itemFormDto){
        this.itemNm = itemFormDto.getItemNm();
        this.price = itemFormDto.getPrice();
        this.stockNumber = itemFormDto.getStockNumber();
        this.itemDetail = itemFormDto.getItemDetail();
        this.itemSellStatus = itemFormDto.getItemSellStatus();
    }

    // 17추가
    public void updateSellItem(ItemToSellDto itemToSellDto){
        System.out.println("=== Item.updateSellItem() 시작 ===");
        
        String title = itemToSellDto.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            this.itemNm = title;
        }
        this.price = itemToSellDto.getPrice();
        // 추가 코드
        this.stockNumber = itemToSellDto.getStockNumber();
        // 개수? 추가?
        this.itemSellStatus = itemToSellDto.getItemSellStatus();
        
        // 사이즈별 재고 정보 설정
        if (itemToSellDto.getSizeStock() != null && !itemToSellDto.getSizeStock().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                this.sizeStockJson = objectMapper.writeValueAsString(itemToSellDto.getSizeStock());
                System.out.println("사이즈별 재고 JSON 설정 완료: " + this.sizeStockJson);
            } catch (Exception e) {
                System.err.println("사이즈별 재고 JSON 변환 실패: " + e.getMessage());
            }
        } else {
            System.out.println("사이즈별 재고 정보가 없습니다.");
        }
        
        System.out.println("=== Item.updateSellItem() 완료 ===");
    }



    public void removeStock(int stockNumber){
        System.out.println("=== Item.removeStock() 시작 ===");
        System.out.println("현재 재고: " + this.stockNumber + ", 차감할 수량: " + stockNumber);
        
        int restStock = this.stockNumber - stockNumber; // 10, 5 / 10 , 20
        if(restStock<0){
            System.err.println("재고 부족! 현재 재고: " + this.stockNumber + ", 요청 수량: " + stockNumber);
            throw new OutOfStockException("상품의 재고가 부족합니다.(현재 재고 수량: "+this.stockNumber+")");
        }
        this.stockNumber = restStock; // 5
        System.out.println("재고 차감 완료, 남은 재고: " + this.stockNumber);
    }

    public void addStock(int stockNumber){
        this.stockNumber += stockNumber;
    }
}
