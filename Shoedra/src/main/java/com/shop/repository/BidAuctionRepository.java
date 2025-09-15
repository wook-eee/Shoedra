package com.shop.repository;

import com.shop.dto.BidHistoryDto;
import com.shop.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BidAuctionRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByAuctionIdOrderByBidPrice(Long auctionId);

    // 최고 입찰가 조회
    @Query("SELECT MAX(b.bidPrice) FROM Bid b WHERE b.auction.id = :auctionId")
    Integer findHighestBidPriceByAuctionId(@Param("auctionId") Long auctionId);

    // 또 다시 입찰하는사람인지
    boolean existsByMemberIdAndAuctionId(Long memberId, Long auctionId);

    //응찰내역
    @Query("SELECT new com.shop.dto.BidHistoryDto(a.id, a.item.itemNm, b.bidPrice, b.bidTime, " +
            "CASE WHEN b.bidPrice = (" +
            "   SELECT MAX(bb.bidPrice) FROM Bid bb WHERE bb.auction.id = a.id" +
            ") AND a.auctionState = com.shop.constant.AuctionState.ENDED THEN true ELSE false END) " +
            "FROM Bid b JOIN b.auction a " +
            "WHERE b.member.email = :email " +
            "ORDER BY b.bidTime DESC")
    Page<BidHistoryDto> findBidHistoryByMemberEmail(@Param("email") String email, Pageable pageable);

    //낙찰내역
    @Query("SELECT new com.shop.dto.BidHistoryDto(a.id, a.item.itemNm, b.bidPrice, b.bidTime, true) " +
            "FROM Bid b JOIN b.auction a " +
            "WHERE b.member.email = :email " +
            "AND a.auctionState = com.shop.constant.AuctionState.ENDED " +
            "AND b.status = com.shop.constant.BidStatus.WINNER " +
            "AND b.bidPrice = (" +
            "   SELECT MAX(bb.bidPrice) FROM Bid bb WHERE bb.auction.id = a.id" +
            ") " +
            "ORDER BY b.bidTime DESC")
    Page<BidHistoryDto> findWinningBidsByMemberEmail(@Param("email") String email, Pageable pageable);

    // 경매상품의 최고입찰 내역
    Bid findTopByAuctionIdOrderByBidPriceDesc(Long auctionId);


}
