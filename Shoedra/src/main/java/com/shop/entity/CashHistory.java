package com.shop.entity;

import com.shop.constant.CashHistoryType;
import com.shop.constant.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashHistory {

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("false")
    private Boolean refunded = false;  // ✅ 또는 @ColumnDefault("false")

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private int amount;

    @Enumerated(EnumType.STRING)
    private CashHistoryType cashHistoryType;

    private String reason;

    @Getter
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private PaymentMethod method;

    private String impUid;
}
