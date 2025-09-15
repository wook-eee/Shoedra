package com.shop.service;

import com.shop.dto.CartDetailDto;
import com.shop.dto.CartItemDto;
import com.shop.dto.CartOrderDto;
import com.shop.dto.OrderDto;
import com.shop.entity.*;
import com.shop.repository.*;
import com.shop.exception.OutOfStockException;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderService orderService;
    private final IamportClient iamportClient; // ✅ 결제 검증용
    private final BidAuctionRepository bidAuctionRepository;
    private final ItemImgRepository itemImgRepository; // ✅ 이미지 조회용

    /**
     * ✅ 장바구니에 상품 추가 (개선된 버전)
     * @param cartItemDto 장바구니 아이템 정보
     * @param email 사용자 이메일
     * @return 장바구니 아이템 ID
     */
    public Long addCart(@Valid CartItemDto cartItemDto, String email){
        log.info("=== 장바구니 추가 시작 ===");
        log.info("상품ID: {}, 수량: {}, 사이즈: {}, 사용자: {}", 
                cartItemDto.getItemId(), cartItemDto.getCount(), cartItemDto.getSize(), email);
        
        // ✅ 1. 상품 존재 여부 확인
        Item item = itemRepository.findById(cartItemDto.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));
        
        // ✅ 2. 사용자 확인
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        
        // ✅ 3. 사이즈 유효성 검증
        if (!cartItemDto.isValidSize()) {
            throw new IllegalArgumentException("유효하지 않은 사이즈입니다: " + cartItemDto.getSize());
        }
        
        // ✅ 4. 재고 확인
        validateStock(item, cartItemDto.getCount(), cartItemDto.getSize());
        
        // ✅ 5. 장바구니 조회 또는 생성
        Cart cart = cartRepository.findByMemberId(member.getId());
        if(cart == null){
            cart = Cart.createCart(member);
            cartRepository.save(cart);
            log.info("새로운 장바구니 생성: {}", cart.getId());
        }
        
        // ✅ 6. 기존 장바구니 아이템 확인 (사이즈 정보 포함)
        CartItem savedCartItem = null;
        if (cartItemDto.getSize() != null) {
            // 사이즈가 있는 경우: 상품+사이즈 조합으로 찾기
            savedCartItem = cartItemRepository.findByCartIdAndItemIdAndSize(
                    cart.getId(), item.getId(), cartItemDto.getSize());
        } else {
            // 사이즈가 없는 경우: 기존 방식으로 찾기
            savedCartItem = cartItemRepository.findByCartIdAndItemId(cart.getId(), item.getId());
        }
        
        // ✅ 7. 기존 아이템이 있으면 수량 추가, 없으면 새로 생성
        if(savedCartItem != null){
            log.info("기존 장바구니 아이템 발견, 수량 추가: {} -> {}", 
                    savedCartItem.getCount(), savedCartItem.getCount() + cartItemDto.getCount());
            
            // 추가 수량에 대한 재고 재확인
            validateStock(item, savedCartItem.getCount() + cartItemDto.getCount(), cartItemDto.getSize());
            
            savedCartItem.addCount(cartItemDto.getCount());
            return savedCartItem.getId();
        } else {
            log.info("새로운 장바구니 아이템 생성");
            CartItem cartItem = CartItem.createCartItem(cart, item, cartItemDto.getCount(), cartItemDto.getSize());
            cartItemRepository.save(cartItem);
            return cartItem.getId();
        }
    }

    /**
     * ✅ 재고 검증
     */
    private void validateStock(Item item, int count, Integer size) {
        // 사이즈별 재고가 있는 경우
        if (size != null && item.getSizeStockJson() != null && !item.getSizeStockJson().isEmpty()) {
            try {
                // TODO: JSON에서 해당 사이즈 재고 확인 로직 구현
                // 현재는 전체 재고로 확인
                if (item.getStockNumber() < count) {
                    throw new OutOfStockException(
                            String.format("재고가 부족합니다. 요청: %d개, 재고: %d개", count, item.getStockNumber()));
                }
            } catch (Exception e) {
                log.warn("사이즈별 재고 확인 실패, 전체 재고로 확인: {}", e.getMessage());
                if (item.getStockNumber() < count) {
                    throw new OutOfStockException(
                            String.format("재고가 부족합니다. 요청: %d개, 재고: %d개", count, item.getStockNumber()));
                }
            }
        } else {
            // 전체 재고로 확인
            if (item.getStockNumber() < count) {
                throw new OutOfStockException(
                        String.format("재고가 부족합니다. 요청: %d개, 재고: %d개", count, item.getStockNumber()));
            }
        }
    }

    /**
     * ✅ 장바구니 목록 조회 (사이즈 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<CartDetailDto> getCartList(String email){
        log.info("장바구니 목록 조회: {}", email);
        
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Cart cart = cartRepository.findByMemberId(member.getId());
        if(cart == null){
            log.info("장바구니가 없습니다: {}", email);
            return new ArrayList<>();
        }
        
        // ✅ 사이즈 정보 포함하여 조회
        List<CartDetailDto> cartItems = cartItemRepository.findCartDetailDtoListWithSize(cart.getId());
        log.info("장바구니 아이템 수: {}", cartItems.size());
        
        return cartItems;
    }

    /**
     * ✅ 장바구니 아이템 권한 검증
     */
    @Transactional(readOnly = true)
    public boolean validateCartItem(Long cartItemId, String email){
        Member curMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("장바구니 아이템을 찾을 수 없습니다."));
        
        Member savedMember = cartItem.getCart().getMember();
        return StringUtils.equals(curMember.getEmail(), savedMember.getEmail());
    }

    /**
     * ✅ 장바구니 아이템 수량 변경
     */
    public void updateCartItemCount(Long cartItemId, int count, String email) {
        log.info("장바구니 아이템 수량 변경: cartItemId={}, count={}, email={}", cartItemId, count, email);
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("장바구니 항목이 존재하지 않습니다."));

        // ✅ 본인 검증
        if (!cartItem.getCart().getMember().getEmail().equals(email)) {
            throw new SecurityException("본인의 장바구니만 수정할 수 있습니다.");
        }
        
        // ✅ 재고 확인
        validateStock(cartItem.getItem(), count, cartItem.getSize());
        
        cartItem.updateCount(count);
        log.info("장바구니 아이템 수량 변경 완료: {}", count);
    }

    /**
     * ✅ 장바구니 아이템 삭제
     */
    public void deleteCartItem(Long cartItemId, String email) {
        log.info("장바구니 아이템 삭제: cartItemId={}, email={}", cartItemId, email);
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("장바구니 항목이 존재하지 않습니다."));

        if (!cartItem.getCart().getMember().getEmail().equals(email)) {
            throw new SecurityException("본인의 장바구니만 삭제할 수 있습니다.");
        }

        cartItemRepository.delete(cartItem);
        log.info("장바구니 아이템 삭제 완료");
    }

    /**
     * ✅ 장바구니 항목 결제 및 주문 처리 (개선된 버전)
     */
    public Long orderCartItem(List<CartOrderDto> cartOrderDtoList, String email){
        log.info("=== 장바구니 주문 처리 시작 ===");
        log.info("주문 아이템 수: {}, 사용자: {}", cartOrderDtoList.size(), email);
        
        if(cartOrderDtoList == null || cartOrderDtoList.isEmpty()){
            throw new IllegalArgumentException("주문할 상품이 없습니다.");
        }

        // ✅ 1. 결제 정보 추출 (첫 번째 DTO 기준)
        CartOrderDto firstDto = cartOrderDtoList.get(0);
        String impUid = firstDto.getImpUid();
        String merchantUid = firstDto.getMerchantUid();
        String payMethod = firstDto.getPayMethod();
        int amount = firstDto.getAmount();

        // ✅ 2. 결제 검증
        try {
            IamportResponse<Payment> response = iamportClient.paymentByImpUid(impUid);
            Payment payment = response.getResponse();

            if (!"paid".equals(payment.getStatus()) || payment.getAmount().intValue() != amount) {
                throw new IllegalStateException("결제 검증 실패: 상태 또는 금액 불일치");
            }
            log.info("결제 검증 성공: {}", impUid);
        } catch (Exception e) {
            log.error("결제 검증 실패: {}", e.getMessage());
            throw new IllegalStateException("결제 검증 중 오류 발생: " + e.getMessage());
        }

        // ✅ 3. 주문 처리
        List<OrderDto> orderDtoList = new ArrayList<>();
        for(CartOrderDto cartOrderDto : cartOrderDtoList){
            CartItem cartItem = cartItemRepository.findById(cartOrderDto.getCartItemId())
                    .orElseThrow(() -> new EntityNotFoundException("장바구니 아이템을 찾을 수 없습니다."));
            
            // ✅ 재고 재확인
            validateStock(cartItem.getItem(), cartItem.getCount(), cartItem.getSize());
            
            OrderDto orderDto = new OrderDto();
            orderDto.setItemId(cartItem.getItem().getId());
            orderDto.setCount(cartItem.getCount());
            // ✅ 사이즈 정보도 포함 (OrderDto에 size 필드가 있다면)
            // orderDto.setSize(cartItem.getSize());
            orderDtoList.add(orderDto);
        }

        // ✅ 4. 결제 완료 주문 저장
        Long orderId = orderService.ordersWithPayment(orderDtoList, email, impUid, amount, merchantUid, payMethod);
        log.info("주문 저장 완료: orderId={}", orderId);

        // ✅ 5. 장바구니 아이템 삭제
        for(CartOrderDto cartOrderDto : cartOrderDtoList){
            CartItem cartItem = cartItemRepository.findById(cartOrderDto.getCartItemId())
                    .orElseThrow(() -> new EntityNotFoundException("장바구니 아이템을 찾을 수 없습니다."));
            cartItemRepository.delete(cartItem);
        }
        
        log.info("장바구니 아이템 삭제 완료");
        log.info("=== 장바구니 주문 처리 완료 ===");

        return orderId;
    }

    /**
     * ✅ 특정 장바구니 아이템 조회
     * @param cartItemId 장바구니 아이템 ID
     * @param email 사용자 이메일
     * @return 장바구니 아이템 정보
     */
    @Transactional(readOnly = true)
    public CartDetailDto getCartItemById(Long cartItemId, String email) {
        log.info("장바구니 아이템 조회: cartItemId={}, email={}", cartItemId, email);
        
        // ✅ 권한 검증
        if (!validateCartItem(cartItemId, email)) {
            throw new SecurityException("본인의 장바구니만 조회할 수 있습니다.");
        }
        
        // ✅ 장바구니 아이템 조회
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("장바구니 아이템을 찾을 수 없습니다: " + cartItemId));
        
        // ✅ CartDetailDto로 변환
        CartDetailDto cartDetailDto = new CartDetailDto(
                cartItem.getItem().getId(),
                cartItem.getId(),
                cartItem.getItem().getItemNm(),
                cartItem.getItem().getPrice(),
                cartItem.getCount(),
                getItemImageUrl(cartItem.getItem().getId()),
                cartItem.getSize()
        );
        
        log.info("장바구니 아이템 조회 완료: {}", cartDetailDto.getItemNm());
        return cartDetailDto;
    }
    
    /**
     * ✅ 상품 이미지 URL 조회
     */
    private String getItemImageUrl(Long itemId) {
        try {
            // 대표 이미지 조회
            List<ItemImg> itemImgs = itemImgRepository.findByItemIdOrderByIdAsc(itemId);
            if (!itemImgs.isEmpty()) {
                // 대표 이미지가 있으면 사용
                for (ItemImg itemImg : itemImgs) {
                    if ("Y".equals(itemImg.getRepImgYn())) {
                        return itemImg.getImgUrl();
                    }
                }
                // 대표 이미지가 없으면 첫 번째 이미지 사용
                return itemImgs.get(0).getImgUrl();
            }
            return "/images/default.jpg"; // 기본 이미지
        } catch (Exception e) {
            log.warn("상품 이미지 조회 실패: itemId={}, error={}", itemId, e.getMessage());
            return "/images/default.jpg";
        }
    }
}