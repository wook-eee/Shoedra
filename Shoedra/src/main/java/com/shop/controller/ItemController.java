package com.shop.controller;

import com.shop.constant.AuctionState;
import com.shop.constant.ItemSellStatus;
import com.shop.dto.ItemDto;
import com.shop.dto.ItemFormDto;
import com.shop.dto.ItemSearchDto;
import com.shop.dto.ItemToSellDto;
import com.shop.entity.Auction;
import com.shop.entity.Item;
import com.shop.repository.AuctionRepository;
import com.shop.service.ItemService;
import com.shop.service.JsoupService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    private final AuctionRepository auctionRepository;

    private final JsoupService jsoupService;

    @GetMapping(value = "/admin/item/new")
    public String itemForm(Model model){
        model.addAttribute("itemFormDto",new ItemFormDto());
        return "item/itemForm";
    }
    @PostMapping(value = "/admin/item/new")
    public String itemNew(@Valid ItemFormDto itemFormDto, BindingResult bindingResult, Model model,
                          @RequestParam("itemImgFile") List<MultipartFile> itemImgFileList,
                          @RequestParam Map<String, String> allParams){
        if(bindingResult.hasErrors()){
            return "item/itemForm";
        }
        if(itemImgFileList.get(0).isEmpty() && itemFormDto.getId() == null){
            model.addAttribute("errorMessage",
                    "첫번째 상품 이미지는 필수 입력 값입니다.");
            return "item/itemForm";
        }
        
        // 사이즈별 재고 데이터 처리
        Map<String, Integer> sizeStock = new HashMap<>();
        for (int size = 220; size <= 280; size += 5) {
            String paramName = "sizeStock[" + size + "]";
            String value = allParams.get(paramName);
            if (value != null && !value.trim().isEmpty()) {
                try {
                    sizeStock.put(String.valueOf(size), Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    // 숫자가 아닌 경우 0으로 처리
                    sizeStock.put(String.valueOf(size), 0);
                }
            } else {
                sizeStock.put(String.valueOf(size), 0);
            }
        }
        itemFormDto.setSizeStock(sizeStock);
        
        try {
            itemService.saveItem(itemFormDto, itemImgFileList);
        }catch (Exception e){
            model.addAttribute("errorMessage",
                    "상품 등록 중 에러가 발생하였습니다.");
            return "item/itemForm";
        }
        return "redirect:/";
    }
    @GetMapping(value = "/admin/item/{itemId}")
    public String itemDtl(@PathVariable("itemId")Long itemId, Model model){
        try {
            ItemFormDto itemFormDto = itemService.getItemDtl(itemId);
            model.addAttribute("itemFormDto",itemFormDto);
        }catch (EntityNotFoundException e){
            model.addAttribute("errorMessage","존재하지 않는 상품입니다.");
            model.addAttribute("itemFormDto",new ItemFormDto());
            return "item/itemForm";
        }
        return "item/itemForm";
    }

    @PostMapping(value = "/admin/item/{itemId}")
    public String itemUpdate(@Valid ItemFormDto itemFormDto, BindingResult bindingResult,
                             @RequestParam("itemImgFile") List<MultipartFile> itemImgFileList,
                             @RequestParam Map<String, String> allParams,
                             Model model){
        if (bindingResult.hasErrors()){
            return "item/itemForm";
        }
        if (itemImgFileList.get(0).isEmpty() && itemFormDto.getId() == null){
            model.addAttribute("errorMessage", "첫번째 상품 이미지는 필수 입력 값입니다.");
            return "item/itemForm";
        }
        
        // 사이즈별 재고 데이터 처리
        Map<String, Integer> sizeStock = new HashMap<>();
        for (int size = 220; size <= 280; size += 5) {
            String paramName = "sizeStock[" + size + "]";
            String value = allParams.get(paramName);
            if (value != null && !value.trim().isEmpty()) {
                try {
                    sizeStock.put(String.valueOf(size), Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    // 숫자가 아닌 경우 0으로 처리
                    sizeStock.put(String.valueOf(size), 0);
                }
            } else {
                sizeStock.put(String.valueOf(size), 0);
            }
        }
        itemFormDto.setSizeStock(sizeStock);
        
        try {
            itemService.updateItem(itemFormDto, itemImgFileList);
        }catch (Exception e){
            model.addAttribute("errorMessage","상품 수정 중 에러가 발생하였습니다.");
            return "item/itemForm";
        }
        return "redirect:/"; // 다시실행
    }
    //value 2개인 이유
    //1. 네비게이션에서 상품관리 클릭하면 나오는거
    //2. 상품관리안에서 페이지 이동할 때 받는거

    @GetMapping(value = {"/admin/items", "/admin/items/{page}"})
    public String itemManage(ItemSearchDto itemSearchDto,
                             @PathVariable("page")Optional<Integer> page,
                                Model model){

//        Pageable pageable = PageRequest.of(page.orElse(0), 5);
//
//        Page<Item> items = itemService.getAdminItemPage(itemSearchDto, pageable);
//
//        model.addAttribute("items",items);
//        model.addAttribute("itemSearchDto", itemSearchDto);
//        model.addAttribute("maxPage",5);

        return "item/itemMng";
    }


    @GetMapping("/admin/items/load")
    @ResponseBody
    public Page<ItemDto> getItemPageApi(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "12") int size,
                                        ItemSearchDto searchDto) {
        System.out.println("음?"+ page +","+size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Item> result = itemService.getAdminItemPage(searchDto, pageable);

        // Page<Item> → Page<ItemDto>로 매핑하여 반환
        return result.map(item -> {
            Auction latestAuction = auctionRepository.findFirstByItemIdOrderByStartTimeDesc(item.getId()).orElse(null);
            String auctionState = latestAuction != null ? latestAuction.getAuctionState().name() : null;

            return new ItemDto(
                    item.getId(),
                    item.getItemNm(),
                    item.getPrice(),
                    item.getRepImgUrl(),
                    auctionState, // 경매 여부 전달
                    item.getItemSellStatus().name()
            );

        });
    }

    //17추가
    @GetMapping(value = {"/item/sell-items", "/item/sell-items/{page}"})
    public String sellItemManage(ItemSearchDto itemSearchDto,
                                 @PathVariable("page")Optional<Integer> page,
                                 Model model){

        Pageable pageable = PageRequest.of(page.orElse(0), 5);

        //Page<Item> items = itemService.getSellItemPage(ItemSellStatus.DATA, pageable);

        Page<ItemDto> items = itemService.getSellItemPage(ItemSellStatus.DATA, pageable);


        model.addAttribute("items",items);
        model.addAttribute("itemSearchDto", itemSearchDto);
        model.addAttribute("maxPage",5);


        return "item/sellItemMng";
    }



    @GetMapping(value = "/item/{itemId}")
    public String itemDtl(Model model, @PathVariable("itemId")Long itemId){
        ItemFormDto itemFormDto = itemService.getItemDtl(itemId);
        model.addAttribute("item",itemFormDto);

        return "item/itemDtl";
    }

    // 17추가 itemToSellDto추가함, 서비스추가, Item엔티티에 내용추가
    @PostMapping(value = "/admin/sell/new")
    public String cellRegister(@Valid ItemToSellDto itemToSellDto,
                               BindingResult bindingResult,
                               @RequestParam Map<String, String> allParams,
                               Model model){

        System.out.println("=== 판매 상품 등록 시작 ===");
        System.out.println("상품ID: " + itemToSellDto.getDataItemId());
        System.out.println("가격: " + itemToSellDto.getPrice());
        System.out.println("색상: " + itemToSellDto.getColor());
        System.out.println("전체 파라미터: " + allParams);

        if (bindingResult.hasErrors()) {
            System.err.println("검증 오류: " + bindingResult.getAllErrors());
            model.addAttribute("errorMessage", "입력 데이터에 오류가 있습니다.");
            return "item/itemMng";
        }
        
        // 사이즈별 재고 데이터 처리
        Map<String, Integer> sizeStock = new HashMap<>();
        for (int size = 220; size <= 280; size += 5) {
            String paramName = "sizeStock[" + size + "]";
            String value = allParams.get(paramName);
            if (value != null && !value.trim().isEmpty()) {
                try {
                    sizeStock.put(String.valueOf(size), Integer.parseInt(value));
                    System.out.println("사이즈 " + size + "mm: " + value + "개");
                } catch (NumberFormatException e) {
                    // 숫자가 아닌 경우 0으로 처리
                    sizeStock.put(String.valueOf(size), 0);
                    System.out.println("사이즈 " + size + "mm: 0개 (숫자 변환 실패)");
                }
            } else {
                sizeStock.put(String.valueOf(size), 0);
                System.out.println("사이즈 " + size + "mm: 0개 (값 없음)");
            }
        }
        itemToSellDto.setSizeStock(sizeStock);
        
        // 전체 재고 계산
        int totalStock = sizeStock.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        itemToSellDto.setStockNumber(totalStock);
        
        System.out.println("전체 재고: " + totalStock);
        System.out.println("사이즈별 재고 정보: " + sizeStock);

        try {
            Long itemId = itemService.itemToSell(itemToSellDto);
            System.out.println("상품 등록 성공! 상품ID: " + itemId);
        } catch (Exception e) {
            System.err.println("상품 등록 실패: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "상품 등록 중 오류가 발생했습니다: " + e.getMessage());
            return "item/itemMng";
        }

        System.out.println("=== 판매 상품 등록 완료 ===");
        return "redirect:/item/sell-items";
    }


    @PostMapping("/admin/data")
    @ResponseBody
    public ResponseEntity<String> doSomething() {
        String url = "https://www.shoemarker.co.kr/ASP/Product/New.asp?SCode1=01";
        try {
            //List<String> jsoupItems = jsoupService.getListItems(url);
            jsoupService.getListItems(url);
            //단일상품 테스트용2
            //String test = jsoupService.getDetailData(url);

            return ResponseEntity.ok("처리 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리 실패");
        }
    }



}
