package com.shop.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CalendarDto {
    private Long id;
    private String title;
    private String start; // 날짜 문자열 (yyyy-MM-dd)
    private String end;
}
