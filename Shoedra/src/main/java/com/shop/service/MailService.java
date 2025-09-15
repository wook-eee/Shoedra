package com.shop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    // 인증코드 생성
    public String createCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase(); // 6자리 코드
    }

    public void sendAuthCode(String toEmail, String authCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("회원가입 인증코드");
        message.setText("인증코드: " + authCode);
        mailSender.send(message);
    }
//    public void sendEventNotification(String toEmail, String eventTitle) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(toEmail);
//        message.setSubject("새 일정 알림");
//        message.setText("새로운 일정이 등록되었습니다: " + eventTitle);
//        mailSender.send(message);
//    }

}