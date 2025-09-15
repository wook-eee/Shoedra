package com.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class OrderItem extends BaseEntity{
    @Id
    @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")//외래키
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")//외래키
    private Order order;

    private int orderPrice;
    private int count;

   /* private LocalDateTime regTime;
    private LocalDateTime updateTime;
*/

    public static OrderItem createOrderItem(Item item, int count){
        System.out.println("=== OrderItem.createOrderItem() 시작 ===");
        System.out.println("Item: " + item.getItemNm() + ", Count: " + count + ", 현재 재고: " + item.getStockNumber());
        
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setCount(count);
        orderItem.setOrderPrice(item.getPrice());
        
        System.out.println("재고 차감 시작");
        item.removeStock(count);
        System.out.println("재고 차감 완료, 남은 재고: " + item.getStockNumber());
        
        return orderItem;
    }

    public int getTotalPrice(){
        return orderPrice * count;
    }

    public void cancel(){
        this.getItem().addStock(count);
    }
}
