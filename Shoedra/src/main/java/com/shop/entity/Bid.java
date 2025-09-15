package com.shop.entity;

import com.shop.constant.AuctionState;
import com.shop.constant.BidStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Bid {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(nullable = false)
    private int bidPrice;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime bidTime;

    @Enumerated(EnumType.STRING)
    private BidStatus status;

    //낙찰자
    public void updateStatusWinner(){
        this.status = BidStatus.valueOf("WINNER");
    }
    //결재완료
    public void updateStatusPaid(){
        this.status = BidStatus.valueOf("PAID");
    }
}
