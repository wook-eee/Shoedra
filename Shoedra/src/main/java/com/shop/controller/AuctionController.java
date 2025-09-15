package com.shop.controller;

import com.shop.dto.AuctionBidInfoDto;
import com.shop.dto.AuctionFormDto;
import com.shop.dto.AuctionListDto;
import com.shop.dto.CalendarDto;
import com.shop.entity.Auction;
import com.shop.entity.Item;
import com.shop.repository.ItemRepository;
import com.shop.service.AuctionService;
import com.shop.service.BidService;
import com.shop.service.CalendarService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class AuctionController {
    private final AuctionService auctionService;
    private final BidService bidService;

    private final ItemRepository itemRepository;
    private final CalendarService calendarService;


    @GetMapping(value = "/auction/auctions")
    public String auctionList(){

        return "auction/auctions";
    }

    @GetMapping("/auction/auctions/load")
    @ResponseBody
    public Page<AuctionListDto> loadAuctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());

        return auctionService.getAuctionPage(pageable);
    }

    @GetMapping("/auction/{auctionId}")
    public String itemDtl(Model model, @PathVariable("auctionId")Long auctionId, Principal principal){
        // 나중에 principal 적용하기
        System.out.println("principal::"+principal.getName());

        Auction auction = auctionService.getAuctionById(auctionId);

        auction.getItem().getRepImgUrl();

        model.addAttribute("auction",auction);

        return "auction/auctionDetail";
    }


    @GetMapping("/auction/{auctionId}/connected-users")
    @ResponseBody
    public Set<String> getConnectedUsers(@PathVariable Long auctionId) {
        return bidService.getConnectedUsers(auctionId);
    }

    @GetMapping(value = "/auction/bid")
    @ResponseBody
    public Map<String, Object> detailBid(@RequestParam("id") Long auctionId ,
                                         @RequestParam("end") LocalDateTime endTime) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        //List<BidDetailDto> auctionBidList = bidAuctionRepository.findBidDetailDtoList(auctionId);
        AuctionBidInfoDto auctionBidInfoDto = bidService.getAuctionBidInfo(auctionId);

        LocalDateTime currentTime = LocalDateTime.now();

        Duration duration = Duration.between(currentTime, endTime);

        long remainingSeconds = duration.getSeconds();

        System.out.println("남은 초: " + remainingSeconds);
        //System.out.println(duration.toHours() + "시간 " + duration.toMinutesPart() + "분 " + duration.toSecondsPart() + "초");

        if(remainingSeconds <= 0){
            auctionService.updateDtlStatus(auctionId);
            bidService.updateStatusWinner(auctionId);
        }



        map.put("restTime", remainingSeconds);
        map.put("auctionBidInfo", auctionBidInfoDto);

        return map;

    }



    @PostMapping(value = "/admin/auction/new")
    public String actionRegister(@Valid AuctionFormDto auctionFormDto,
                                 BindingResult bindingResult,
                                 Model model){

        if (bindingResult.hasErrors()) {
            return "admin/items"; // 오류 발생 시 폼으로 되돌림
        }

        try {
            auctionService.saveAuction(auctionFormDto);


            // 경매상품 등록시 캘린더에 경매일정 등록
            CalendarDto calDto = new CalendarDto();

            String strStartDate = auctionFormDto.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String strEndDate = auctionFormDto.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            Item item = itemRepository.findById(auctionFormDto.getItemId()).orElseThrow(EntityNotFoundException::new);


            calDto.setTitle("경매 상품: "+item.getItemNm());
            calDto.setStart(strStartDate);
            calDto.setEnd(strEndDate);

            // 경매일정 등록
            calendarService.addEvent(calDto);


        } catch (Exception e) {
            model.addAttribute("errorMessage", "경매 등록 중 오류가 발생했습니다.");
            return "admin/items";
        }

        return "redirect:/auction/auctions";
    }

}
