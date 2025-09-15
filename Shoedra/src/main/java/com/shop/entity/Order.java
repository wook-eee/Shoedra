package com.shop.entity;

import com.shop.constant.OrderStatus;
import com.shop.constant.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    // ✅ PortOne imp_uid 저장
    @Column(name = "payment_key")
    private String paymentKey;

    // ✅ PortOne 결제 수단 (card, vbank 등)
    @Column(name = "payment_method")
    private String paymentMethod;

    // ✅ 결제 완료 시간
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    private String merchantUid;

    private Boolean refunded;
//    private LocalDateTime refundedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderAuction> orderAuctions = new ArrayList<>();

    // ✅ 주문 고유 식별자 (merchant_uid)
    @Column(name = "order_uuid", unique = true)
    private String orderId;

    // [추가] 포인트(캐시) 사용 금액
    private Integer useCash;
    public Integer getUseCash() { return useCash; }
    public void setUseCash(Integer useCash) { this.useCash = useCash; }

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    // 주문 아이템을 주문에 추가
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    // 주문 생성
    public static Order createOrder(Member member, List<OrderItem> orderItemList) {
        Order order = new Order();
        order.setMember(member);
        for (OrderItem orderItem : orderItemList) {
            order.addOrderItem(orderItem);
        }
        order.setOrderStatus(OrderStatus.ORDER);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    public static Order createAuctionOrder(Member member, List<OrderAuction> orderAuctionList) {
        Order order = new Order();
        order.setMember(member);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.ORDER);

        for (OrderAuction orderAuction : orderAuctionList) {
            orderAuction.setOrder(order); // 양방향 연결
            order.getOrderAuctions().add(orderAuction);
        }

        return order;
    }

    // 총 주문 금액 계산
    public int getTotalPrice() {
        int totalPrice = 0;
        for (OrderItem orderItem : orderItems) {
            totalPrice += orderItem.getTotalPrice();
        }
        return totalPrice;
    }

    // 주문 취소
    public void cancelOrder() {
        this.orderStatus = OrderStatus.CANCEL;
        for (OrderItem orderItem : orderItems) {
            orderItem.cancel();
        }
    }
}
