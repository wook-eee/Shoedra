package com.shop.repository;

import com.shop.constant.AuctionState;
import com.shop.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Optional<Auction> findFirstByItemIdOrderByStartTimeDesc(Long itemId);

    @Query("SELECT a FROM Auction a " +
            "JOIN FETCH a.item i " +
            "LEFT JOIN FETCH i.itemImgs " +
            "WHERE a.id = :id")
    Optional<Auction> findWithItemAndImagesById(@Param("id") Long id);

    List<Auction> findByIdIn(List<Long> auctionIds);
}
