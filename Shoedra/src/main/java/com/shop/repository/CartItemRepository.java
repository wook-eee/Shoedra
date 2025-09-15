package com.shop.repository;

import com.shop.dto.CartDetailDto;
import com.shop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * ✅ 장바구니에서 특정 상품 찾기 (사이즈 정보 없이)
     */
    CartItem findByCartIdAndItemId(Long cartId, Long itemId);
    
    /**
     * ✅ 장바구니에서 특정 상품+사이즈 조합 찾기
     */
    CartItem findByCartIdAndItemIdAndSize(Long cartId, Long itemId, Integer size);
    
    /**
     * ✅ 장바구니 상세 정보 조회 (기존 - 사이즈 정보 없음)
     */
    @Query("select new com.shop.dto.CartDetailDto(i.id, ci.id, i.itemNm, i.price, ci.count, im.imgUrl) " +
            "from CartItem ci, ItemImg im "+ 
            "join ci.item i "+
            "where ci.cart.id = :cartId "+
            "and im.item.id = ci.item.id "+
            "and im.repImgYn = 'Y' "+
            "order by ci.regTime desc")
    List<CartDetailDto> findCartDetailDtoList(Long cartId);
    
    /**
     * ✅ 장바구니 상세 정보 조회 (사이즈 정보 포함)
     */
    @Query("select new com.shop.dto.CartDetailDto(i.id, ci.id, i.itemNm, i.price, ci.count, im.imgUrl, ci.size) " +
            "from CartItem ci, ItemImg im "+ 
            "join ci.item i "+
            "where ci.cart.id = :cartId "+
            "and im.item.id = ci.item.id "+
            "and im.repImgYn = 'Y' "+
            "order by ci.regTime desc")
    List<CartDetailDto> findCartDetailDtoListWithSize(Long cartId);
    
    /**
     * ✅ 특정 상품의 장바구니 아이템 수 조회
     */
    @Query("select count(ci) from CartItem ci where ci.cart.id = :cartId and ci.item.id = :itemId")
    long countByCartIdAndItemId(@Param("cartId") Long cartId, @Param("itemId") Long itemId);
}
