package com.shop.entity;

import com.shop.constant.AuctionState;
import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction extends BaseEntity{
    @Id
    @Column(name = "auction_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id; // 상품코드

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    private int startPrice;
    private int currentBidPrice;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private AuctionState auctionState;




    @PrePersist
    public void prePersist() {
        if (this.auctionState == null) {
            this.auctionState = AuctionState.READY;
        }

        if (this.currentBidPrice == 0) {
            this.currentBidPrice = this.startPrice;
        }
    }

    public void updateCurrentPrice(int bidPrice) {
        this.currentBidPrice = bidPrice;

    }

    public void updateStatus(){
        this.auctionState = AuctionState.valueOf("ENDED");
    }



}
