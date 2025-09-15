package com.shop.service;

import com.shop.constant.ItemSellStatus;
import com.shop.dto.AuctionFormDto;
import com.shop.entity.Item;
import com.shop.entity.ItemImg;
import com.shop.repository.ItemImgRepository;
import com.shop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class JsoupService {
    @Value("${itemImgLocation}") //application.properties에 itemImgLocation
    private String itemImgLocation;

    private final ItemImgRepository itemImgRepository;
    private final ItemRepository itemRepository;


    public List<String> getListItems(String url) throws IOException {
        // url : https://kream.co.kr/search?tab=44&tmp=1751939878139
        // select : div.search_result_list
        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000)
                .get();

        //System.out.println(document.html());

        Elements items = document.select("div.area-sorting div.wrap-product-list ul>li");
        // System.out.println(items.html());

        List<String> dataList = new ArrayList<>();

        System.out.println("사이즈:"+items.size());


        for (Element item : items) {
            String id = item.attr("id");
            String text = item.attr("data-productcode");
            String name = item.select(".ly-name").text();
            String price = item.select(".ly-price>.ns-type-bl-eb18x").text();
            String detailUrl = item.select(".ly-img a").attr("href");
            String imgUrl = item.select(".ly-img img").attr("src");


            String strStartPrice = price.replace(" 품절", "");
            int startPrice = Integer.parseInt(strStartPrice.replace(",",""));


            System.out.println("id: "+id);
            System.out.println("번호: "+text);
            System.out.println("이름?: "+name);
            System.out.println("가격: "+price);
            System.out.println("---------------");
            System.out.println("변경후: "+startPrice);
            System.out.println("상세url: "+detailUrl);
            System.out.println("이미지대표url: "+imgUrl);




            getDetailData(detailUrl);



        }

        return dataList;
    }

    public String getDetailData(String detailUrl) throws IOException{
        Document document = Jsoup.connect("https://www.shoemarker.co.kr/"+detailUrl).get();
        //Document document = Jsoup.connect(detailUrl).get();
        Elements elements = document.select("#innerDetail .detail_contents img");

        // 이미지 저장용 리스트
        List<String> imageUrls = new ArrayList<>();

        //상세내용 img들
        System.out.println("변경전:"+elements);

        // 대표이미지
        String imgUrl = document.select("#ProductImage").attr("src");

        imageUrls.add(imgUrl);

        //대표이미지 url
        String frontUrl = imgUrl.substring(0, imgUrl.lastIndexOf("/") + 1);
        System.out.println("앞에:"+frontUrl);


        // 상세이미지 첫번째 이미지경로
        String firstUrl = Objects.requireNonNull(elements.first()).attr("src");

        System.out.println("상세이미지첫번째url:"+firstUrl);

        String updateDtlUrl = firstUrl.substring(0, firstUrl.lastIndexOf("/") + 1);

        System.out.println("수정부분:"+updateDtlUrl);

        // 상세내용 이미지경로 변경
        String originHtml = String.valueOf(elements);
        String updateHtml = originHtml.replaceAll(updateDtlUrl, "/images/item/");

        System.out.println("변경후:"+updateHtml);

        String id = document.select(".product-detail-chk-wrap #product-detail-fav").val();

        String title = document.select(".detail-top .title-wrap .title").first().text();;

        String price = document.select(".detail-top .right .title-wrap .price>strong").first().text();

        int startPrice = Integer.parseInt(price.replace(",", ""));

        System.out.println("아이디:"+id);
        System.out.println("제목:"+title);
        System.out.println("가격:"+startPrice);




        Item item = new Item();

        //item.setId(Long.valueOf(id));
        item.setItemNm(title);
        item.setPrice(startPrice);
        item.setItemDetail(updateHtml);

        //-------------------고정

        item.setStockNumber(1);

        item.setItemSellStatus(ItemSellStatus.valueOf("DATA"));


        System.out.println("아이템:"+item);

        // Item insert func

        itemRepository.save(item);

        // 대표이미지

        String mainUrl = imgUrl.replace(frontUrl, "/images/item/");

        String mainFileName = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);

        ItemImg mainItemImg = new ItemImg();

        mainItemImg.setImgUrl(mainUrl);
        mainItemImg.setImgName(mainFileName);
        mainItemImg.setOriImgName(mainFileName);
        mainItemImg.setRepImgYn("Y");
        mainItemImg.setItem(item);

        System.out.println("메인이미지:"+mainUrl);
        System.out.println(mainItemImg);


        itemImgRepository.save(mainItemImg);









        /*

        //System.out.println("앞에:"+frontUrl);

        String originHtml = String.valueOf(elements);

        String updateHtml = originHtml.replaceAll(frontUrl, "/images/item/");

        System.out.println("================================");
        System.out.println(updateHtml);


         */


        //item.setItemDetail(updateHtml);

        //System.out.println("아이템:"+item);





        System.out.println("-------------------------------");
        System.out.println("엘리먼츠 사이즈:"+elements.size());

        for (Element element : elements){
            ItemImg itemImg = new ItemImg();

            //System.out.println(element);
            String url = element.attr("src");
            //System.out.println(url);

            String updateUrl = url.replaceAll(updateDtlUrl, "/images/item/");

            //System.out.println("----후----");

            System.out.println(updateUrl);


            String fileName = url.substring(url.lastIndexOf("/") + 1);
            //System.out.println("파일이름:"+fileName);
            imageUrls.add(url);
            itemImg.setRepImgYn("N");
            itemImg.setItem(item);
            itemImg.setImgName(fileName);
            itemImg.setOriImgName(fileName);
            itemImg.setImgUrl(updateUrl);

            //System.out.println("이미지정보:"+itemImg);

            itemImgRepository.save(itemImg);
        }

        //System.out.println(imageUrls);

        //downloadImages(imageUrls, itemImgLocation);





        return "";
    }

    public String updateHtmlImgSrc(String html, String baseUrl) {
        // 외부 이미지 경로를 내부 경로로 치환
        return html.replaceAll("", baseUrl);
    }

    public void downloadImages(List<String> imageUrls, String saveDir) throws IOException {
        for (String urlStr : imageUrls) {
            URL url = new URL(urlStr);
            String fileName = urlStr.substring(urlStr.lastIndexOf("/") + 1);

            System.out.println("경론가:"+saveDir + File.separator + fileName);

            try (InputStream in = url.openStream();
                 OutputStream out = new FileOutputStream(saveDir + File.separator + fileName))
            {
                in.transferTo(out);
            }
        }
    }
}
