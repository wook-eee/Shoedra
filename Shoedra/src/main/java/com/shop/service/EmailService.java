package com.shop.service;

import com.shop.entity.EmailToken;
import com.shop.repository.EmailTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final EmailTokenRepository emailTokenRepository;

    public EmailService(JavaMailSender mailSender, EmailTokenRepository emailTokenRepository) {
        this.mailSender = mailSender;
        this.emailTokenRepository = emailTokenRepository;
    }

    @Value("${spring.mail.username}")
    private String fromEmail;

    // 인증 메일 전송
    public void sendVerificationEmail(String email, HttpServletRequest request) {
        String token = generateToken();
        saveToken(email, token);

        String verificationLink = generateLink(token, request);

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail); // 발신자
        message.setTo(email); // 수신자
        message.setSubject("이메일 인증"); // 메일 제목

        // 메일 본문
        message.setText("안녕하세요,\\n\\n아래 링크를 클릭하여 이메일 인증을 완료해주세요.\\n\\n인증 링크: " + verificationLink);
        mailSender.send(message);
    }

    // 토큰 생성
    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    // 토큰 저장
    private void saveToken(String email, String token) {
        EmailToken emailToken = new EmailToken();
        emailToken.setToken(token);
        emailToken.setEmail(email);

        // 만료 기간을 10분으로 설정
        emailToken.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        emailTokenRepository.save(emailToken);
    }

    // 인증 링크 생성
    private String generateLink(String token, HttpServletRequest request) {
        return getBaseUrl(request) + "/verify/" + token;
    }

    private static String getBaseUrl(HttpServletRequest request) {
        return request.getRequestURL().toString().replace(request.getRequestURI(), "");
    }

    public boolean isTokenExpired(String token) {
        EmailToken emailToken = emailTokenRepository.findByToken(token);
        if (emailToken == null) {
            return true; // 토큰이 존재하지 않으면 만료된 것으로 간주
        }

        LocalDateTime expiryDate = emailToken.getExpiryDate();
        return expiryDate.isBefore(LocalDateTime.now());
    }

    public boolean isVerifiedEmail(String email) {
        EmailToken emailToken = emailTokenRepository.findByEmail(email);

        if (emailToken == null || !emailToken.isVerified()) {
            return false;
        }

        return true;
    }

    // 이메일의 인증된 상태로 변경
    public void verifySuccess(String token) {
        emailTokenRepository.updateVerifiedByToken(token);
    }

    // 1분마다 실행
    @Scheduled(cron = "0 * * * * ?")
    public void deleteExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        emailTokenRepository.deleteByExpiryDateBefore(now);
    }

}
