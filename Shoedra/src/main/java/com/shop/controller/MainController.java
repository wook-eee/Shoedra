package com.shop.controller;

import com.shop.dto.AuctionListDto;
import com.shop.dto.ItemSearchDto;
import com.shop.dto.MainItemDto;
import com.shop.service.AuctionService;
import com.shop.service.ItemService;
import com.shop.service.JsoupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final ItemService itemService;
    private final JsoupService jsoupService;
    private final AuctionService auctionService;

    @GetMapping(value = "/")
    public String main(ItemSearchDto itemSearchDto, Optional<Integer> page, Model model){
        Pageable pageable = PageRequest.of(page.isPresent() ? page.get() : 0, 5);

        // 경매상품
        Page<AuctionListDto> auctionItems = auctionService.getAuctionPage(pageable);

        model.addAttribute("items", auctionItems);

        // 그냥 복붙한듯?
        model.addAttribute("maxPage", 5);
        model.addAttribute("itemSearchDto", itemSearchDto);


        // 판매상품 데이터 (경매가 등록된 상품들만)
        Pageable sellPageable = PageRequest.of(0, 10); // 판매상품은 첫 페이지에서 10개만
        //Page<MainItemDto> sellItems = itemService.getMainItemPage(itemSearchDto, sellPageable);
        Page<MainItemDto> Items = itemService.getMainItemPage(itemSearchDto, sellPageable);

        model.addAttribute("sellItems", Items);




        //크롤링용 코드

        /*
        String url = "https://www.shoemarker.co.kr/ASP/Product/New.asp?SCode1=01";

        //단일상품 테스트용1
        String url = "https://www.shoemarker.co.kr/ASP/Product/ProductDetail.asp?ProductCode=47919";

        try {
            List<String> jsoupItems = jsoupService.getListItems(url);
            //단일상품 테스트용2
            //String test = jsoupService.getDetailData(url);
        }catch (IOException e) {
            e.printStackTrace();
        }

         */


        return "main";
    }


}
