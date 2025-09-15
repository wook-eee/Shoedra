package com.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.constant.ItemSellStatus;
import com.shop.dto.*;
import com.shop.entity.Item;
import com.shop.entity.ItemImg;
import com.shop.repository.ItemImgRepository;
import com.shop.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final ItemImgService itemImgService;
    private final ItemImgRepository itemImgRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public Long saveItem(ItemFormDto itemFormDto, List<MultipartFile> itemImgFileList)
            throws Exception{
        //상품등록
        Item item = itemFormDto.createItem();
        
        // 사이즈별 재고 처리
        if (itemFormDto.getSizeStock() != null && !itemFormDto.getSizeStock().isEmpty()) {
            // 전체 재고 계산
            int totalStock = itemFormDto.getSizeStock().values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            item.setStockNumber(totalStock);
            
            // 사이즈별 재고 정보를 JSON으로 저장
            try {
                String sizeStockJson = objectMapper.writeValueAsString(itemFormDto.getSizeStock());
                item.setSizeStockJson(sizeStockJson);
                System.out.println("사이즈별 재고 정보 (JSON): " + sizeStockJson);
            } catch (JsonProcessingException e) {
                System.err.println("사이즈별 재고 JSON 변환 실패: " + e.getMessage());
            }
        }
        
        itemRepository.save(item);
        //이미지 등록
        for(int i =0;i<itemImgFileList.size();i++){ // 5번 반복
            ItemImg itemImg = new ItemImg();
            itemImg.setItem(item);
            if(i==0)
                itemImg.setRepImgYn("Y"); // 0
            else
                itemImg.setRepImgYn("N"); // 1 2 3 4
            itemImgService.saveItemImg(itemImg,itemImgFileList.get(i));
        }
        return item.getId();
    }
    @Transactional(readOnly = true)
    public ItemFormDto getItemDtl(Long itemId){
        //Entity
        List<ItemImg> itemImgList = itemImgRepository.findByItemIdOrderByIdAsc(itemId);
        //Db에서 데이터를 가지고 옵니다.
        //DTO
        List<ItemImgDto> itemImgDtoList = new ArrayList<>(); //왜 DTO 만들었나요?

        for (ItemImg itemImg : itemImgList){
            //Entity > DTO
            ItemImgDto itemImgDto = ItemImgDto.of(itemImg);
            itemImgDtoList.add(itemImgDto);
        }
        Item item = itemRepository.findById(itemId).orElseThrow(EntityNotFoundException::new);
        //Item > ItemFormDto modelMapper
        ItemFormDto itemFormDto = ItemFormDto.of(item);
        itemFormDto.setItemImgDtoList(itemImgDtoList);
        return itemFormDto;
    }

    public Long updateItem(ItemFormDto itemFormDto, List<MultipartFile> itemImgFileList) throws Exception {
        //상품변경
        Item item = itemRepository.findById(itemFormDto.getId()).orElseThrow(EntityNotFoundException::new);
        
        // 사이즈별 재고 처리
        if (itemFormDto.getSizeStock() != null && !itemFormDto.getSizeStock().isEmpty()) {
            // 전체 재고 계산
            int totalStock = itemFormDto.getSizeStock().values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            itemFormDto.setStockNumber(totalStock);
            
            // 사이즈별 재고 정보를 JSON으로 저장
            try {
                String sizeStockJson = objectMapper.writeValueAsString(itemFormDto.getSizeStock());
                item.setSizeStockJson(sizeStockJson);
                System.out.println("사이즈별 재고 정보 (JSON): " + sizeStockJson);
            } catch (JsonProcessingException e) {
                System.err.println("사이즈별 재고 JSON 변환 실패: " + e.getMessage());
            }
        }
        
        //상품이미지변경
        item.updateItem(itemFormDto);

        List<Long> itemImgIds = itemFormDto.getItemImgIds();

        for (int i = 0; i < itemImgFileList.size(); i++) {
            itemImgService.updateItemImg(itemImgIds.get(i), itemImgFileList.get(i));
        }
        return item.getId();
    }

    public Long itemToSell(ItemToSellDto itemToSellDto) {
        System.out.println("=== ItemService.itemToSell() 시작 ===");
        
        Item item = itemRepository.findById(itemToSellDto.getDataItemId()).orElseThrow(EntityNotFoundException::new);
        System.out.println("기존 상품 찾음: " + item.getItemNm());

        String updateTitle = item.getItemNm(); // 기본값으로 원래 상품명 사용

        if (itemToSellDto.getColor() != null && !itemToSellDto.getColor().trim().isEmpty()) {
            updateTitle = item.getItemNm() + " | " + itemToSellDto.getColor();
        }

        itemToSellDto.setTitle(updateTitle);
        itemToSellDto.setItemSellStatus(ItemSellStatus.valueOf("SELL"));

        System.out.println("상품 정보 업데이트 전:");
        System.out.println("- 상품명: " + item.getItemNm());
        System.out.println("- 가격: " + item.getPrice());
        System.out.println("- 재고: " + item.getStockNumber());
        System.out.println("- 상태: " + item.getItemSellStatus());

        item.updateSellItem(itemToSellDto);

        System.out.println("상품 정보 업데이트 후:");
        System.out.println("- 상품명: " + item.getItemNm());
        System.out.println("- 가격: " + item.getPrice());
        System.out.println("- 재고: " + item.getStockNumber());
        System.out.println("- 상태: " + item.getItemSellStatus());
        System.out.println("- 사이즈별 재고 JSON: " + item.getSizeStockJson());

        System.out.println("판매 상품 등록 - 상품ID: " + itemToSellDto.getDataItemId());
        System.out.println("가격: " + itemToSellDto.getPrice());
        System.out.println("색상: " + itemToSellDto.getColor());
        System.out.println("전체 재고: " + itemToSellDto.getStockNumber());
        System.out.println("사이즈별 재고 정보: " + itemToSellDto.getSizeStock());

        System.out.println("=== ItemService.itemToSell() 완료 ===");
        return item.getId();
    }



    @Transactional(readOnly = true) // 쿼리문 실행 읽기만 한다.
    public Page<Item> getAdminItemPage(ItemSearchDto itemSearchDto, Pageable pageable){
        return itemRepository.findAllWithRepImage(itemSearchDto, pageable);
    }

    //17추가
    @Transactional(readOnly = true) // 쿼리문 실행 읽기만 한다.
    public Page<ItemDto> getSellItemPage(ItemSellStatus itemSellStatus , Pageable pageable){
        Page<Item> itemPage = itemRepository.findByItemSellStatusNotOrderByUpdateTimeDesc(itemSellStatus, pageable);

        List<ItemDto> dtoList = itemPage.getContent().stream()
                .map(ItemDto::new)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, itemPage.getTotalElements());
    }


    @Transactional(readOnly = true)
    public Page<MainItemDto> getMainItemPage(ItemSearchDto itemSearchDto, Pageable pageable){
        return itemRepository.getMainItemPage(itemSearchDto, pageable);
    }
}
