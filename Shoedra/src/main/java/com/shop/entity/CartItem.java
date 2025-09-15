package com.shop.entity;


import jakarta.persistence.*;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "cart_item")
public class CartItem extends BaseEntity{

    @Id
    @GeneratedValue
    @Column(name = "cart_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;
    
    private int count;
    
    // ✅ 사이즈 정보 추가 (신발 사이즈: 220, 225, 230, ..., 280)
    private Integer size;

    /**
     * ✅ 장바구니 아이템 생성 (사이즈 정보 포함)
     * @param cart 장바구니
     * @param item 상품
     * @param count 수량
     * @param size 사이즈 (mm 단위)
     * @return 생성된 CartItem
     */
    public static CartItem createCartItem(Cart cart, Item item, int count, Integer size){
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setItem(item);
        cartItem.setCount(count);
        cartItem.setSize(size);
        return cartItem;
    }

    /**
     * ✅ 수량 추가 (기존 메서드 유지)
     */
    public void addCount(int count){
        this.count += count;
    }

    /**
     * ✅ 수량 업데이트
     */
    public void updateCount(int count){ 
        this.count = count; 
    }
    
    /**
     * ✅ 총 가격 계산
     */
    public int getTotalPrice() {
        return this.item.getPrice() * this.count;
    }
    
    /**
     * ✅ 재고 확인
     */
    public boolean hasEnoughStock() {
        // 사이즈별 재고가 있는 경우 해당 사이즈 재고 확인
        if (this.size != null && this.item.getSizeStockJson() != null) {
            try {
                // JSON에서 사이즈별 재고 확인 로직 (추후 구현)
                return this.item.getStockNumber() >= this.count;
            } catch (Exception e) {
                // JSON 파싱 실패 시 전체 재고로 확인
                return this.item.getStockNumber() >= this.count;
            }
        }
        // 사이즈 정보가 없으면 전체 재고로 확인
        return this.item.getStockNumber() >= this.count;
    }
}
