package com.shop.controller;

import com.shop.dto.MemberFormDto;
import com.shop.entity.Member;
import com.shop.service.MemberService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/members")
@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    @GetMapping(value = "/new")
    public String memberForm(Model model){
        model.addAttribute("memberFormDto",new MemberFormDto());
        return "member/memberForm";
    }

    // 이메일 인증코드 발송 API
    @PostMapping("/send-code")
    @ResponseBody
    public String sendVerificationCode(@RequestParam("email") String email) throws MessagingException {
        memberService.sendVerificationCode(email);
        return "ok";
    }

    // 이메일 인증코드 확인 API
    @PostMapping("/verify-code")
    @ResponseBody
    public String verifyCode(@RequestParam("email") String email, @RequestParam("code") String code) {
        boolean result = memberService.verifyCode(email, code);
        return result ? "success" : "fail";
    }



    @PostMapping(value = "/new")
    public String memberForm(@Valid MemberFormDto memberFormDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "member/memberForm";
        }
        // 이메일 인증코드 검증
        if (memberFormDto.getVerificationCode() == null || !memberService.verifyCode(memberFormDto.getEmail(), memberFormDto.getVerificationCode())) {
            model.addAttribute("errorMessage", "이메일 인증코드가 올바르지 않습니다.");
            return "member/memberForm";
        }
        try {
            Member member = Member.createMember(memberFormDto, passwordEncoder);
            memberService.saveMember(member);
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "member/memberForm";
        }
        return "redirect:/";
    }


    @GetMapping(value = "/login")
    public String loginMember() {
        return "/member/memberLoginForm";
    }

    @GetMapping(value = "/login/error")
    public String loginError(Model model) {
        model.addAttribute("loginErrorMsg", "아이디 또는 비밀번호를 확인해주세요");
        return "/member/memberLoginForm";
    }

    // 이메일 중복 체크 API
    @GetMapping("/check-email")
    @ResponseBody
    public boolean checkEmailDuplicate(@RequestParam("email") String email) {
        return memberService.isEmailDuplicate(email);
    }

    // 관리자 권한 부여 및 비밀번호 변경 (임시)
    @PostMapping("/grant-admin")
    @ResponseBody
    public String grantAdmin(@RequestParam("email") String email, @RequestParam("password") String password) {
        memberService.grantAdminAndSetPassword(email, password);
        return "ok";
    }


}
