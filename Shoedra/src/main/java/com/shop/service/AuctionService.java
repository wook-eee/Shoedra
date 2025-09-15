package com.shop.service;

import com.shop.constant.AuctionState;
import com.shop.dto.AuctionFormDto;
import com.shop.dto.AuctionListDto;
import com.shop.dto.AuctionOrderDto;
import com.shop.entity.Auction;
import com.shop.entity.Item;
import com.shop.entity.ItemImg;
import com.shop.repository.AuctionRepository;
import com.shop.repository.ItemImgRepository;
import com.shop.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Builder
public class AuctionService {
    private final ItemRepository itemRepository;
    private final AuctionRepository auctionRepository;
    private final ItemImgRepository itemImgRepository;

    public void saveAuction(AuctionFormDto auctionFormDto){
        Item item = itemRepository.findById(auctionFormDto.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();

        AuctionState state;
        if (auctionFormDto.getStartTime().isAfter(now)) {
            state = AuctionState.READY;
        } else if (auctionFormDto.getEndTime().isAfter(now)) {
            state = AuctionState.IN_PROGRESS;
        } else {
            state = AuctionState.ENDED;
        }



        Auction auction = Auction.builder()
                .item(item)
                .startPrice(auctionFormDto.getStartPrice())
                .currentBidPrice(auctionFormDto.getStartPrice())
                .startTime(auctionFormDto.getStartTime())
                .endTime(auctionFormDto.getEndTime())
                .auctionState(state)
                .build();
//        Auction auction = new Auction();
//        auction.setItem(item);
//        auction.setStartPrice(auctionFormDto.getStartPrice());
//        auction.setStartTime(auctionFormDto.getStartTime());
//        auction.setEndTime(auctionFormDto.getEndTime());

        auctionRepository.save(auction);
    }



    public Page<AuctionListDto> getAuctionPage(Pageable pageable) {
        Page<Auction> auctionPage = auctionRepository.findAll(pageable);

        return auctionPage.map(auction -> {
            // 각 경매의 대표 이미지 조회
            Item item = auction.getItem();
            ItemImg repImg = itemImgRepository.findFirstByItemIdAndRepImgYn(item.getId(), "Y");
            String repImgUrl = repImg != null ? repImg.getImgUrl() : "/images/no-image.png";

            return new AuctionListDto(auction, repImgUrl);
        });
    }

    public Auction getAuctionById(Long auctionId){
        return auctionRepository.findWithItemAndImagesById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 경매가 존재하지 않습니다."));
    }

    public void updateDtlStatus(Long auctionId) throws Exception{
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(EntityNotFoundException::new);

        auction.updateStatus();
    }

    public List<AuctionOrderDto> getAuctionOrderDtos(List<Long> auctionIds) {
        List<Auction> auctions = auctionRepository.findByIdIn(auctionIds);
        return auctions.stream()
                .map(AuctionOrderDto::new)
                .collect(Collectors.toList());
    }
}
