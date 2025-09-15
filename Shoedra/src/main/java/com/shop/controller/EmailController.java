package com.shop.controller;

import com.shop.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;

    @PostMapping("/sendEmail")
    public ResponseEntity<Void> sendVerificationEmail(@RequestParam("email") String email, HttpServletRequest request) {
        System.out.println("음?"+email);

        emailService.sendVerificationEmail(email, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/verify/{token}")
    public String verifyEmail(@PathVariable String token) {

        // 토큰 만료
        if (emailService.isTokenExpired(token)) {
            return "verifyError";
        }

        emailService.verifySuccess(token);

        return "verifySuccess";
    }

}
