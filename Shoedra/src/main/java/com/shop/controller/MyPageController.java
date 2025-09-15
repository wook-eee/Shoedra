package com.shop.controller;

import com.shop.dto.BidHistoryDto;
import com.shop.dto.MemberFormDto;
import com.shop.entity.Member;
import com.shop.repository.MemberRepository;
import com.shop.service.BidService;
import com.shop.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;
    private final BidService bidService;

    // 마이페이지 메인
    @GetMapping("")
    public String myPageMain(Principal principal, Model model) {
        //Member member = memberService.findByEmail(principal.getName());
        Member member = memberRepository.findByEmail(principal.getName()).orElseThrow(null);
        model.addAttribute("member", member);
        return "mypage/main";
    }

//    // 프로필 수정 페이지
//    @GetMapping("/profile")
//    public String profileForm(Principal principal, Model model) {
//        Member member = memberService.findByEmail(principal.getName());
//        MemberFormDto memberFormDto = new MemberFormDto();
//        memberFormDto.setName(member.getName());
//        memberFormDto.setEmail(member.getEmail());
//        memberFormDto.setAddress(member.getAddress());
//        memberFormDto.setPhone(member.getPhone());
//
//        model.addAttribute("memberFormDto", memberFormDto);
//        return "mypage/profile";
//    }
//
//    // 프로필 수정 처리
//    @PostMapping("/profile")
//    public String updateProfile(@Valid MemberFormDto memberFormDto,
//                                BindingResult bindingResult,
//                                Principal principal,
//                                Model model) {
//        if (bindingResult.hasErrors()) {
//            return "mypage/profile";
//        }
//
//        try {
//            memberService.updateProfile(principal.getName(), memberFormDto);
//            return "redirect:/mypage?success=profile";
//        } catch (Exception e) {
//            model.addAttribute("errorMessage", e.getMessage());
//            return "mypage/profile";
//        }
//    }
//
//    // 비밀번호 변경 페이지
//    @GetMapping("/password")
//    public String passwordForm() {
//        return "mypage/password";
//    }
//
//    // 비밀번호 변경 처리
//    @PostMapping("/password")
//    public String updatePassword(@RequestParam String currentPassword,
//                                 @RequestParam String newPassword,
//                                 @RequestParam String confirmPassword,
//                                 Principal principal,
//                                 Model model) {
//        try {
//            memberService.updatePassword(principal.getName(), currentPassword, newPassword, confirmPassword);
//            return "redirect:/mypage?success=password";
//        } catch (Exception e) {
//            model.addAttribute("errorMessage", e.getMessage());
//            return "mypage/password";
//        }
//    }
//
//    // 회원 탈퇴 페이지
//    @GetMapping("/withdraw")
//    public String withdrawForm() {
//        return "mypage/withdraw";
//    }
//
//    // 회원 탈퇴 처리
//    @PostMapping("/withdraw")
//    public String withdrawMember(@RequestParam String password,
//                                 Principal principal,
//                                 Model model) {
//        try {
//            memberService.withdrawMember(principal.getName(), password);
//            return "redirect:/members/logout";
//        } catch (Exception e) {
//            model.addAttribute("errorMessage", e.getMessage());
//            return "mypage/withdraw";
//        }
//    }
//
    // 응찰내역 페이지
    @GetMapping("/bid-history")
    public String bidHistory(Model model, Principal principal,
                             @PageableDefault(size = 5) Pageable pageable) {

        String email = principal.getName();
        Page<BidHistoryDto> historyPage = bidService.getBidHistory(email, pageable);

        model.addAttribute("historyPage", historyPage);
        model.addAttribute("currentPage", historyPage.getNumber());
        model.addAttribute("totalPages", historyPage.getTotalPages());

        return "mypage/bidHistory";
    }
    // 낙찰내역 페이지
    @GetMapping("/win-history")
    public String winHistory(Model model, Principal principal,
                             @PageableDefault(size = 5) Pageable pageable) {
        String email = principal.getName();
        Page<BidHistoryDto> winPage = bidService.getWinningBids(email, pageable);

        model.addAttribute("historyPage", winPage);
        model.addAttribute("currentPage", winPage.getNumber());
        model.addAttribute("totalPages", winPage.getTotalPages());

        return "mypage/winHistory";
    }

//
//    // 1:1채팅 페이지
//    @GetMapping("/chat")
//    public String chat() {
//        return "mypage/chat";
//    }
}