package com.shop.dto;

import com.shop.constant.BidStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BidDto {
    private String bidderNickname;
    private int bidPrice;
    private LocalDateTime bidTime;
    private BidStatus status;
}
