package com.shop.service;

import com.shop.constant.BidStatus;
import com.shop.dto.AuctionBidInfoDto;
import com.shop.dto.BidDto;
import com.shop.dto.BidHistoryDto;
import com.shop.entity.Auction;
import com.shop.entity.Bid;
import com.shop.entity.Member;
import com.shop.repository.AuctionRepository;
import com.shop.repository.BidAuctionRepository;
import com.shop.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {
    private final BidAuctionRepository bidAuctionRepository;
    private final AuctionRepository auctionRepository;

    private final MemberRepository memberRepository;

    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> auctionEmitters = new ConcurrentHashMap<>();


    public AuctionBidInfoDto getAuctionBidInfo(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매 상품이 존재하지 않습니다."));
        List<Bid> bids = bidAuctionRepository.findByAuctionIdOrderByBidPrice(auctionId);

        int currentPrice = bids.isEmpty() ? 0 : bids.get(0).getBidPrice();

        List<BidDto> bidResponseList = bids.stream()
                .map(bid -> new BidDto(
                        bid.getMember().getName(),
                        bid.getBidPrice(),
                        bid.getBidTime(),
                        bid.getStatus()
                ))
                .collect(Collectors.toList());

        System.out.println("시작가:"+auction.getStartPrice());

        return new AuctionBidInfoDto(auctionId, auction.getItem().getItemNm(),
                                    currentPrice, auction.getStartPrice() ,bidResponseList);
    }

    @Transactional
    public int addBid(Long memberId, Long auctionId, int bidPrice){

        System.out.println("경매아이디:"+auctionId);
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매 상품이 존재하지 않습니다."));
        System.out.println("회원아이디:"+memberId);
        //회원 조회 -> Principal이나 @AuthenticationPrincipal 로그인된 사용자 조회 가능하면 수정해도됨
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 존재하지 않습니다."));

        System.out.println("비드값:"+bidPrice);
        // 현재 최고 입찰가 조회
        Integer highestBidPrice = bidAuctionRepository.findHighestBidPriceByAuctionId(auctionId);
        if (highestBidPrice == null) {
            highestBidPrice = auction.getStartPrice(); // 입찰 내역 없으면 시작가
        }


        if (bidPrice <= highestBidPrice) {
            throw new IllegalArgumentException("입찰가는 현재 입찰가보다 높아야 합니다.");
        }

        Bid bid = new Bid();
        bid.setMember(member);
        bid.setAuction(auction);
        bid.setBidPrice(bidPrice);
        bid.setBidTime(LocalDateTime.now());
        bid.setStatus(BidStatus.ACTIVE);

        bidAuctionRepository.save(bid);

        auction.updateCurrentPrice(bidPrice);

        subscribe(auctionId, member.getEmail());



        return bidPrice;
    }

    @Transactional
    public void updateStatusWinner(Long auctionId) throws Exception{
        Bid bid = bidAuctionRepository.findTopByAuctionIdOrderByBidPriceDesc(auctionId);

        if (bid != null) {
            bid.updateStatusWinner();  // 상태 변경
        }

    }


    public SseEmitter subscribe(Long auctionId, String userId) {

        SseEmitter emitter = new SseEmitter(0L);

        auctionEmitters.computeIfAbsent(auctionId, k -> new ConcurrentHashMap<>()).put(userId, emitter);

        emitter.onCompletion(() -> removeEmitter(auctionId, userId));
        emitter.onTimeout(() -> removeEmitter(auctionId, userId));
        emitter.onError(e -> removeEmitter(auctionId, userId));

        System.out.println("auctionEmitters:"+auctionEmitters);
        //System.out.println(auctionEmitters.get(0).keySet());

        return emitter;
    }

    public void removeEmitter(Long auctionId, String userId) {
        ConcurrentHashMap<String, SseEmitter> userEmitters = auctionEmitters.get(auctionId);
        if (userEmitters != null) {
            userEmitters.remove(userId);
            if (userEmitters.isEmpty()) {
                auctionEmitters.remove(auctionId);
            }
        }
    }

    // 특정 경매(auctionId)에 연결된 사용자 ID 리스트 반환
    public Set<String> getConnectedUsers(Long auctionId) {
        ConcurrentHashMap<String, SseEmitter> userEmitters = auctionEmitters.get(auctionId);
        if (userEmitters != null) {
            return userEmitters.keySet();
        }
        return Collections.emptySet();
    }

    // 응찰내역 service
    public Page<BidHistoryDto> getBidHistory(String memberEmail, Pageable pageable) {
        return bidAuctionRepository.findBidHistoryByMemberEmail(memberEmail, pageable);
    }

    // 낙찰내역 service
    public Page<BidHistoryDto> getWinningBids(String memberEmail, Pageable pageable) {
        return bidAuctionRepository.findWinningBidsByMemberEmail(memberEmail, pageable);
    }

}


