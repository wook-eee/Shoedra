package com.shop.controller;

import com.shop.dto.CalendarDto;
import com.shop.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public String showCalendarPage() {
        return "calendar/calendar";
    }

    @GetMapping("/events")
    @ResponseBody
    public List<CalendarDto> getCalendarEvents() {
        return calendarService.getAllEvents();
    }

    @PostMapping("/add")
    @ResponseBody
    public void addEvent(@RequestBody CalendarDto dto) {
        calendarService.addEvent(dto);
    }

    @PostMapping("/update")
    @ResponseBody
    public void updateEvent(@RequestBody CalendarDto dto) {
        calendarService.updateEvent(dto);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public void deleteEvent(@PathVariable Long id) {
        calendarService.deleteEvent(id);

    }
//    @PostMapping("/calendar")
//    public String addCalendar(@ModelAttribute CalendarDto dto) {
//        calendarService.addScheduleAndNotifyAll(dto);
//        return "redirect:/calendar";
//    }
}
