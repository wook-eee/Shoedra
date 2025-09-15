package com.shop.repository;

import com.shop.entity.ItemImg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemImgRepository extends JpaRepository<ItemImg, Long> {
    List<ItemImg> findByItemIdOrderByIdAsc(Long itemId);

    ItemImg findFirstByItemIdAndRepImgYn(Long itemId, String repImgYn);

    //ItemImg findFirstByAuctionIdAndRepImgYn(Long auctionId, String repImgYn);

    @Query("""
    SELECT ii FROM ItemImg ii
    WHERE ii.repImgYn = 'Y'
      AND ii.item.id = (
          SELECT a.item.id FROM Auction a WHERE a.id = :auctionId
      )
    """)
    ItemImg findRepImgByAuctionId(@Param("auctionId") Long auctionId);

}
