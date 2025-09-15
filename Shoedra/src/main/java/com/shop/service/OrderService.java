package com.shop.service;

import com.shop.constant.AuctionState;
import com.shop.constant.BidStatus;
import com.shop.constant.OrderStatus;
import com.shop.dto.*;
//import com.shop.dto.OrderHistDto;
//import com.shop.dto.OrderItemDto;
import com.shop.entity.*;
import com.shop.repository.*;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import com.shop.exception.OutOfStockException;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final ItemImgRepository itemImgRepository;
    private final IamportClient iamportClient;
    private final CashService cashService; // [추가] 포인트(캐시) 차감 처리를 위해 CashService 주입

    private final AuctionRepository auctionRepository;
    private final BidAuctionRepository bidAuctionRepository;


    @Transactional
    public void createOrderFromAuctionList(List<AuctionOrderDto> orderDataList, String email,
                                           String impUid, String merchantUid, String payMethod, int amount) {

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다: " + email));

        // 1. 낙찰 항목들 생성
        List<OrderAuction> orderAuctionList = orderDataList.stream().map(dto -> {
            Auction auction = auctionRepository.findById(dto.getAuctionId())
                    .orElseThrow(() -> new EntityNotFoundException("경매를 찾을 수 없습니다: " + dto.getAuctionId()));

            OrderAuction oa = new OrderAuction();
            oa.setAuction(auction);
            oa.setItemName(auction.getItem().getItemNm());
            oa.setPrice(dto.getWinningPrice());
            return oa;
        }).toList();



        // 2. 👉 Order 생성 (정적 팩토리 메서드 호출)
        Order order = Order.createAuctionOrder(member, orderAuctionList);

        // 3. 추가 정보 세팅
        order.setPaymentKey(impUid);
        order.setPaymentMethod(payMethod);
        order.setMerchantUid(merchantUid);
        order.setApprovedAt(LocalDateTime.now());

        orderRepository.save(order);

        for (AuctionOrderDto auctionOrderDto : orderDataList ){
            Auction auction = auctionRepository.findById(auctionOrderDto.getAuctionId())
                    .orElseThrow(() -> new EntityNotFoundException("경매를 찾을 수 없습니다: " + auctionOrderDto.getAuctionId()));
            Bid winningBid = bidAuctionRepository.findTopByAuctionIdOrderByBidPriceDesc(auction.getId());

            if(winningBid != null){
                winningBid.setStatus(BidStatus.valueOf("PAID"));
            }
            auction.setAuctionState(AuctionState.valueOf("COMPLETE"));
        }



        System.out.println("주문저장");

    }

    public Long order(OrderDto orderDto, String email) {
        try {
            // [수정] impUid가 있을 때만 결제 검증
            if (orderDto.getImpUid() != null && !orderDto.getImpUid().isEmpty()) {
                IamportResponse<Payment> response = iamportClient.paymentByImpUid(orderDto.getImpUid());
                Payment payment = response.getResponse();

                if (!"paid".equals(payment.getStatus()) || payment.getAmount().intValue() != orderDto.getAmount()) {
                    throw new IllegalStateException("결제 검증 실패: 상태 또는 금액 불일치");
                }
            }
            // [수정] impUid가 null 또는 빈 문자열이면 결제 검증 생략

        } catch (Exception e) {
            throw new IllegalStateException("결제 검증 중 오류 발생: " + e.getMessage());
        }

        Item item = itemRepository.findById(orderDto.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다: " + orderDto.getItemId()));
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다: " + email));

        // [추가] 포인트(캐시) 사용 처리
        if (orderDto.getUseCash() != null &&  orderDto.getUseCash() > 0) {
            cashService.useCash(member, orderDto.getUseCash());
        }

        List<OrderItem> orderItemList = new ArrayList<>();
        OrderItem orderItem = OrderItem.createOrderItem(item, orderDto.getCount());
        orderItemList.add(orderItem);

        Order order = Order.createOrder(member, orderItemList);
        order.setOrderStatus(OrderStatus.PAYMENT_COMPLETED);
        order.setPaymentKey(orderDto.getImpUid());
        order.setPaymentMethod(orderDto.getPayMethod());
        order.setMerchantUid(orderDto.getMerchantUid());
        order.setUseCash(orderDto.getUseCash()); // [추가] 사용한 캐시 금액 저장

        orderRepository.save(order);
        return order.getId();
    }


    /*
    @Transactional(readOnly = true)
    public Page<OrderHistDto> getOrderList(String email, Pageable pageable) {
        List<Order> orders = orderRepository.findOrders(email, pageable);
        Long totalCount = orderRepository.countOrder(email);
        List<OrderHistDto> orderHistDtos = new ArrayList<>();

        for (Order order : orders) {
            OrderHistDto orderHistDto = new OrderHistDto(order);
            List<OrderItem> orderItems = order.getOrderItems();
            for (OrderItem orderItem : orderItems) {
                ItemImg itemImg = itemImgRepository.findByItemIdAndRepImgYn(orderItem.getItem().getId(), "Y");
                OrderItemDto orderItemDto = new OrderItemDto(orderItem, itemImg.getImgUrl());
                orderHistDto.addOrderItemDto(orderItemDto);
            }
            orderHistDtos.add(orderHistDto);
        }
        return new PageImpl<>(orderHistDtos, pageable, totalCount);
    }

     */



    @Transactional(readOnly = true)
    public Page<OrderHistDto> getOrderList(String email, Pageable pageable) {
        List<Order> orders = orderRepository.findOrders(email, pageable);
        Long totalCount = orderRepository.countOrder(email);
        List<OrderHistDto> orderHistDtos = new ArrayList<>();

        for (Order order : orders) {
            OrderHistDto orderHistDto = new OrderHistDto(order);
            
            // 일반 상품 주문 처리
            List<OrderItem> orderItems = order.getOrderItems();
            if (orderItems != null && !orderItems.isEmpty()) {
                for (OrderItem orderItem : orderItems) {
                    ItemImg itemImg = itemImgRepository.findFirstByItemIdAndRepImgYn(orderItem.getItem().getId(), "Y");
                    if (itemImg != null) {
                        OrderItemDto orderItemDto = new OrderItemDto(orderItem, itemImg.getImgUrl());
                        orderHistDto.addOrderItemDto(orderItemDto);
                    }
                }
            }
            
            // 경매 상품 주문 처리
            List<OrderAuction> orderAuctions = order.getOrderAuctions();
            if (orderAuctions != null && !orderAuctions.isEmpty()) {
                for (OrderAuction orderAuction : orderAuctions) {
                    ItemImg itemImg = itemImgRepository.findRepImgByAuctionId(orderAuction.getAuction().getId());
                    if (itemImg != null) {
                        OrderAuctionDto orderAuctionDto = new OrderAuctionDto(orderAuction, itemImg.getImgUrl());
                        orderHistDto.addOrderAuctionDto(orderAuctionDto);
                    }
                }
            }

            orderHistDtos.add(orderHistDto);
        }
        return new PageImpl<>(orderHistDtos, pageable, totalCount);
    }




    @Transactional(readOnly = true)
    public boolean validateOrder(Long orderId, String email) {
        Member curMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다: " + email));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다: " + orderId));
        Member savedMember = order.getMember();
        return StringUtils.equals(curMember.getEmail(), savedMember.getEmail());
    }

    /**
     * ✅ 환불 + 주문취소 처리
     */
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다: " + orderId));

        // ✅ 실결제 환불 (카드/포트원 결제)
        if (order.getPaymentKey() != null) {
            try {
                CancelData cancelData = new CancelData(order.getPaymentKey(), true); // 전체 환불
                iamportClient.cancelPaymentByImpUid(cancelData);
            } catch (Exception e) {
                throw new RuntimeException("환불 처리 중 오류 발생: " + e.getMessage());
            }
        }

        // ✅ 포인트(캐시) 환불: 실결제와 상관없이 항상 처리
        Integer useCash = order.getUseCash();
        if (useCash != null && useCash > 0) {
            cashService.refundCash(order.getMember(), useCash);
        }

        order.cancelOrder(); // 주문 상태를 CANCEL로 변경
    }




    public Long orders(List<OrderDto> orderDtoList, String email) {
        log.info("=== OrderService.orders() 시작 ===");
        log.info("Email: {}, OrderDtoList 크기: {}", email, orderDtoList.size());
        
        if (orderDtoList == null || orderDtoList.isEmpty()) {
            throw new IllegalArgumentException("주문할 상품이 없습니다.");
        }
        
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다: " + email));
        
        log.info("Member 조회 완료: {}", member.getName());

        List<OrderItem> orderItemList = new ArrayList<>();
        Integer useCash = orderDtoList.get(0).getUseCash();
        String impUid = orderDtoList.get(0).getImpUid();
        String merchantUid = orderDtoList.get(0).getMerchantUid();
        String payMethod = orderDtoList.get(0).getPayMethod();
        int amount = orderDtoList.get(0).getAmount();
        
        log.info("결제 정보 - useCash: {}, impUid: {}, amount: {}", useCash, impUid, amount);

        // ✅ 포인트(캐시) 사용 처리 (공통)
        if (useCash != null && useCash > 0) {
            log.info("캐시 사용 처리: {}", useCash);
            cashService.useCash(member, useCash);
        }

        // ✅ 결제 검증 (impUid가 있으면)
        if (impUid != null && !impUid.isEmpty()) {
            log.info("결제 검증 시작");
            try {
                IamportResponse<Payment> response = iamportClient.paymentByImpUid(impUid);
                Payment payment = response.getResponse();
                if (!"paid".equals(payment.getStatus()) || payment.getAmount().intValue() != amount) {
                    throw new IllegalStateException("결제 검증 실패: 상태 또는 금액 불일치");
                }
                log.info("결제 검증 성공");
            } catch (Exception e) {
                log.error("결제 검증 실패: {}", e.getMessage());
                throw new IllegalStateException("결제 검증 중 오류 발생: " + e.getMessage());
            }
        } else {
            log.info("결제 검증 생략 (impUid 없음)");
        }

        // ✅ 주문 아이템 생성
        log.info("주문 아이템 생성 시작");
        for (OrderDto orderDto : orderDtoList) {
            log.info("ItemId: {}, Count: {}", orderDto.getItemId(), orderDto.getCount());
            Item item = itemRepository.findById(orderDto.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다: " + orderDto.getItemId()));
            OrderItem orderItem = OrderItem.createOrderItem(item, orderDto.getCount());
            orderItemList.add(orderItem);
        }
        log.info("주문 아이템 생성 완료: {}개", orderItemList.size());

        // ✅ 주문 저장
        log.info("주문 엔티티 생성 시작");
        Order order = Order.createOrder(member, orderItemList);
        order.setOrderStatus(OrderStatus.PAYMENT_COMPLETED);
        order.setPaymentKey(impUid);
        order.setPaymentMethod(payMethod);
        order.setMerchantUid(merchantUid);
        order.setUseCash(useCash);
        
        log.info("주문 저장 시작");
        orderRepository.save(order);
        log.info("주문 저장 완료, OrderId: {}", order.getId());
        return order.getId();
    }


    public void updatePaymentSuccess(String merchantUid, String impUid, Payment payment) {
        Order order = orderRepository.findByOrderId(merchantUid)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다: " + merchantUid));

        order.setOrderStatus(OrderStatus.PAYMENT_COMPLETED);
        order.setPaymentKey(impUid);
        order.setPaymentMethod(payment.getPayMethod());

        Date paidAtDate = payment.getPaidAt();
        if (paidAtDate != null) {
            LocalDateTime paidAt = LocalDateTime.ofInstant(paidAtDate.toInstant(), ZoneId.systemDefault());
            order.setApprovedAt(paidAt);
        }

        orderRepository.save(order);
    }




     // ✅ 장바구니에서 다중 상품 결제 후 주문 저장 + 결제 상태 반영


    /**
     * ✅ 장바구니에서 다중 상품 결제 후 주문 저장 + 결제 상태 반영
     * @param orderDtoList 주문할 상품 목록
     * @param email 사용자 이메일
     * @param impUid 결제 고유 ID
     * @param amount 결제 금액
     * @param merchantUid 주문 고유 ID
     * @param payMethod 결제 방법
     * @return 주문 ID
     */
    public Long ordersWithPayment(List<OrderDto> orderDtoList, String email, String impUid, int amount, String merchantUid, String payMethod) {
        log.info("=== 장바구니 주문 처리 시작 ===");
        log.info("주문 상품 수: {}, 사용자: {}, 결제금액: {}", orderDtoList.size(), email, amount);
        
        // ✅ 1. 사용자 확인
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + email));
        
        // ✅ 2. 주문 아이템 생성
        List<OrderItem> orderItemList = new ArrayList<>();
        for (OrderDto orderDto : orderDtoList) {
            log.info("주문 아이템 처리: itemId={}, count={}", orderDto.getItemId(), orderDto.getCount());
            
            Item item = itemRepository.findById(orderDto.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다: " + orderDto.getItemId()));
            
            // ✅ 재고 확인
            if (item.getStockNumber() < orderDto.getCount()) {
                throw new OutOfStockException(
                        String.format("재고가 부족합니다. 상품: %s, 요청: %d개, 재고: %d개", 
                                item.getItemNm(), orderDto.getCount(), item.getStockNumber()));
            }
            
            OrderItem orderItem = OrderItem.createOrderItem(item, orderDto.getCount());
            orderItemList.add(orderItem);
        }
        
        log.info("주문 아이템 생성 완료: {}개", orderItemList.size());

        // ✅ 3. 주문 생성
        Order order = Order.createOrder(member, orderItemList);

        // ✅ 4. 결제 정보 반영
        order.setOrderStatus(OrderStatus.PAYMENT_COMPLETED);
        order.setPaymentKey(impUid);
        order.setMerchantUid(merchantUid);
        order.setPaymentMethod(payMethod);

        // ✅ 5. 주문 저장
        orderRepository.save(order);
        log.info("주문 저장 완료: orderId={}", order.getId());
        log.info("=== 장바구니 주문 처리 완료 ===");
        
        return order.getId();
    }



}
