package com.shop.service;

import com.shop.dto.CalendarDto;
import com.shop.entity.Calendar;
import com.shop.entity.Member;
import com.shop.repository.CalendarRepository;
import com.shop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;

    private final SmsService smsService;

    private final MemberRepository memberRepository;

    private final MailService mailService;

    public List<CalendarDto> getAllEvents() {
        return calendarRepository.findAll().stream().map(entity -> {
            CalendarDto dto = new CalendarDto();
            dto.setId(entity.getId());
            dto.setTitle(entity.getTitle());
            dto.setStart(entity.getStartDate().toString());
            dto.setEnd(entity.getEndDate() != null ? entity.getEndDate().toString() : null);
            return dto;
        }).collect(Collectors.toList());
    }

    public void addEvent(CalendarDto dto) {

        Calendar calendar = new Calendar();

        calendar.setTitle(dto.getTitle());
        calendar.setStartDate(LocalDate.parse(dto.getStart()));
        calendar.setEndDate(dto.getEnd() != null ? LocalDate.parse(dto.getEnd()) : null);


        calendarRepository.save(calendar);

//        List<Member> members = memberRepository.findAllUsers();
//        for (Member member : members) {
//            mailService.sendEventNotification(member.getEmail(), dto.getTitle()+ " 일정이 등록되었습니다.");
//
//            if (member.getPhone() != null) {
//                smsService.sendSms(member.getPhone(), dto.getTitle() + " 일정이 등록되었습니다.");
//            }
//        }
    }

    public void updateEvent(CalendarDto dto) {
        Calendar calendar = calendarRepository.findById(dto.getId()).orElseThrow();

        calendar.setTitle(dto.getTitle());
        calendar.setStartDate(LocalDate.parse(dto.getStart()));
        calendar.setEndDate(dto.getEnd() != null ? LocalDate.parse(dto.getEnd()) : null);
        calendarRepository.save(calendar);

        // 알림 추가
//        List<Member> members = memberRepository.findAllUsers();
//        for (Member member : members) {
//            mailService.sendEventNotification(member.getEmail(), dto.getTitle() + " 일정이 변경되었습니다.");
//            if (member.getPhone() != null) {
//                smsService.sendSms(member.getPhone(), dto.getTitle() + " 일정이 변경되었습니다.");
//            }
//        }
    }

    public void deleteEvent(Long id) {
        Calendar calendar = calendarRepository.findById(id).orElseThrow();
        String title = calendar.getTitle();  // 삭제 전에 제목 확보

        calendarRepository.deleteById(id);

        //알림 추가
//        List<Member> members = memberRepository.findAllUsers();
//        for (Member member : members) {
//            mailService.sendEventNotification(member.getEmail(), title + " 일정이 삭제되었습니다.");
//            if (member.getPhone() != null) {
//                smsService.sendSms(member.getPhone(), title + " 일정이 삭제되었습니다.");
//            }
//        }
    }
}
