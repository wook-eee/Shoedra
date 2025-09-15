package com.shop.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.dto.*;
import com.shop.entity.Auction;
import com.shop.entity.Item;
import com.shop.entity.ItemImg;
import com.shop.entity.Member;
import com.shop.repository.MemberRepository;
import com.shop.service.*;
//import com.shop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final MemberService memberService;
    private final ItemService itemService;
    private final CartService cartService;

    private final MemberRepository memberRepository;
    private final AuctionService auctionService;

    /**
     * ✅ 주문 처리
     */

    @PostMapping(value = "/order")
    public @ResponseBody ResponseEntity<?> order(
            @RequestBody Map<String, Object> payload,
            Principal principal) {

        System.out.println("=== 주문 요청 받음 ===");
        System.out.println("Principal: " + (principal != null ? principal.getName() : "null"));
        System.out.println("Payload: " + payload);

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String email = principal.getName();
        System.out.println("Email: " + email);

        try {
            // 다중 주문 처리
            if (payload.containsKey("orderList")) {
                System.out.println("다중 주문 처리 시작");
                List<?> orderListRaw = (List<?>) payload.get("orderList");
                List<OrderDto> orderDtoList = new ArrayList<>();

                for (Object obj : orderListRaw) {
                    Map<?, ?> map = (Map<?, ?>) obj;
                    OrderDto dto = new OrderDto();
                    if (map.get("cartItemId") != null) {
                        dto.setCartItemId(Long.parseLong(map.get("cartItemId").toString()));
                    }
                    if (map.get("itemId") != null) {
                        dto.setItemId(Long.parseLong(map.get("itemId").toString()));
                    }
                    if (map.get("count") != null) {
                        dto.setCount(Integer.parseInt(map.get("count").toString()));
                    }
                    // ✅ 프론트에서 "price"로 보낸 값 → amount로 매핑
                    if (map.get("price") != null) {
                        dto.setAmount(Integer.parseInt(map.get("price").toString()));
                    }

                    orderDtoList.add(dto);
                }

                // 공통 결제 정보 세팅
                String impUid = (String) payload.getOrDefault("impUid", "");
                String merchantUid = (String) payload.getOrDefault("merchantUid", "");
                String payMethod = (String) payload.getOrDefault("payMethod", "");
                Integer amount = payload.get("amount") != null ? Integer.parseInt(payload.get("amount").toString()) : 0;
                Integer useCash = payload.get("useCash") != null ? Integer.parseInt(payload.get("useCash").toString()) : 0;

                for (OrderDto dto : orderDtoList) {
                    dto.setImpUid(impUid);
                    dto.setMerchantUid(merchantUid);
                    dto.setPayMethod(payMethod);
                    dto.setAmount(amount); // 전체 금액 통일
                    dto.setUseCash(useCash);
                }

                System.out.println("주문 서비스 호출 전");
                Long orderId = orderService.orders(orderDtoList, email);
                System.out.println("주문 완료, OrderId: " + orderId);
                return ResponseEntity.ok(orderId);
            }

            // 단일 주문 처리
            System.out.println("단일 주문 처리 시작");
            OrderDto orderDto = new OrderDto();
            if (payload.get("itemId") != null) orderDto.setItemId(Long.parseLong(payload.get("itemId").toString()));
            if (payload.get("count") != null) orderDto.setCount(Integer.parseInt(payload.get("count").toString()));
            if (payload.get("impUid") != null) orderDto.setImpUid((String) payload.get("impUid"));
            if (payload.get("merchantUid") != null) orderDto.setMerchantUid((String) payload.get("merchantUid"));
            if (payload.get("payMethod") != null) orderDto.setPayMethod((String) payload.get("payMethod"));
            if (payload.get("amount") != null) orderDto.setAmount(Integer.parseInt(payload.get("amount").toString()));
            if (payload.get("useCash") != null) orderDto.setUseCash(Integer.parseInt(payload.get("useCash").toString()));

            System.out.println("주문 서비스 호출 전");
            Long orderId = orderService.order(orderDto, email);
            System.out.println("주문 완료, OrderId: " + orderId);
            return ResponseEntity.ok(orderId);

        } catch (Exception e) {
            System.err.println("주문 처리 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("주문 처리 실패: " + e.getMessage());
        }
    }


    @PostMapping("/orders/complete")
    @ResponseBody
    public ResponseEntity<?> completeAuctionOrder(@RequestBody Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            String email = principal.getName();

            // ✅ orderDataList 파싱
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) payload.get("orderDataList");
            List<AuctionOrderDto> orderDataList = new ArrayList<>();

            for (Map<String, Object> map : rawList) {
                Long auctionId = Long.parseLong(map.get("auctionId").toString());
                int price = Integer.parseInt(map.get("price").toString());

                AuctionOrderDto dto = new AuctionOrderDto();
                dto.setAuctionId(auctionId);
                dto.setWinningPrice(price);;
                orderDataList.add(dto);
            }

            // ✅ 결제 정보 파싱
            String impUid = (String) payload.getOrDefault("impUid", "");
            String merchantUid = (String) payload.getOrDefault("merchantUid", "");
            String payMethod = (String) payload.getOrDefault("payMethod", "");
            int amount = Integer.parseInt(payload.get("amount").toString());

            // ✅ 서비스 호출
            orderService.createOrderFromAuctionList(orderDataList, email, impUid, merchantUid, payMethod, amount);



            return ResponseEntity.ok("주문 완료");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("주문 처리 실패: " + e.getMessage());
        }
    }





    /**
     * ✅ 주문 내역 조회
     */


    @GetMapping(value = {"/orders", "/orders/{page}"})
    public String orderHist(@PathVariable("page") Optional<Integer> page,
                            Principal principal,
                            Model model) {
        Pageable pageable = PageRequest.of(page.orElse(0), 5);
        Page<OrderHistDto> orderHistDtoList = orderService.getOrderList(principal.getName(), pageable);

        model.addAttribute("orders", orderHistDtoList);
        model.addAttribute("page", pageable.getPageNumber());
        model.addAttribute("maxPage", 5);
        return "order/orderHist";
    }




    /**
     * ✅ 주문 취소
     */

    @PostMapping("/order/{orderId}/cancel")
    public @ResponseBody ResponseEntity<?> cancelOrder(@PathVariable("orderId") Long orderId,
                                                       Principal principal) {
        if (!orderService.validateOrder(orderId, principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("주문 취소 권한이 없습니다.");
        }

        orderService.cancelOrder(orderId);
        return ResponseEntity.ok(orderId);
    }




    /**
     * ✅ 주문서 확인 페이지
     */
    @GetMapping("/order/confirm")
    public String orderConfirm(@RequestParam(required = false) Long itemId,
                               @RequestParam(required = false) Integer count,
                               @RequestParam(required = false) String items,
                               @RequestParam(required = false) Integer size,
                               Model model, Principal principal) {

        if (principal == null) {
            return "redirect:/members/login";
        }

        Member member = memberRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        
        model.addAttribute("member", member);

        // ✅ 장바구니에서 선택된 상품들 처리
        if (items != null && !items.isEmpty()) {
            log.info("장바구니 주문 확인: items={}", items);
            
            String[] itemArr = items.split(",");
            List<CartDetailDto> selectedCartItems = new ArrayList<>();
            
            // ✅ 장바구니 아이템 ID와 수량 파싱
            for (String itemPair : itemArr) {
                String[] parts = itemPair.split(":");
                if (parts.length == 2) {
                    Long cartItemId = Long.parseLong(parts[0]);
                    Integer itemCount = Integer.parseInt(parts[1]);
                    
                    // ✅ 장바구니 아이템 정보 조회
                    try {
                        CartDetailDto cartItem = cartService.getCartItemById(cartItemId, member.getEmail());
                        if (cartItem != null) {
                            cartItem.setCount(itemCount); // 사용자가 변경한 수량으로 업데이트
                            selectedCartItems.add(cartItem);
                        }
                    } catch (Exception e) {
                        log.warn("장바구니 아이템 조회 실패: cartItemId={}, error={}", cartItemId, e.getMessage());
                    }
                }
            }
            
            if (!selectedCartItems.isEmpty()) {
                log.info("선택된 장바구니 아이템 수: {}", selectedCartItems.size());
                
                // ✅ JSON으로 변환하여 프론트엔드에 전달
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String cartItemsJson = objectMapper.writeValueAsString(selectedCartItems);
                    model.addAttribute("cartItemsJson", cartItemsJson);
                    log.info("장바구니 아이템 JSON 생성 완료");
                } catch (Exception e) {
                    log.error("장바구니 아이템 JSON 변환 실패: {}", e.getMessage());
                    model.addAttribute("cartItemsJson", "[]");
                }
                
                model.addAttribute("cartItems", selectedCartItems);
                model.addAttribute("multiOrder", true);
                return "order/orderConfirm";
            } else {
                log.warn("선택된 장바구니 아이템이 없습니다.");
                return "redirect:/cart";
            }
        }

        // ✅ 단일 상품 주문
        if (itemId != null && count != null) {
            try {
                ItemFormDto itemFormDto = itemService.getItemDtl(itemId);
                model.addAttribute("item", itemFormDto);
                model.addAttribute("count", count);
                model.addAttribute("size", size); // 사이즈 정보 추가
                model.addAttribute("multiOrder", false);
                return "order/orderConfirm";
            } catch (Exception e) {
                log.error("단일 상품 주문 확인 실패: itemId={}, error={}", itemId, e.getMessage());
                return "redirect:/";
            }
        }

        // ✅ 파라미터가 없으면 장바구니로 리다이렉트
        return "redirect:/cart";
    }
}
