package com.shop.controller;

import com.shop.dto.PaymentValidationRequestDto;
import com.shop.dto.RefundRequestDto;
import com.shop.entity.CashHistory;
import com.shop.entity.Member;
import com.shop.repository.MemberRepository;
import com.shop.service.CashService;
import com.shop.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final CashService cashService;


    // 캐시 페이지 (잔액 + 버튼)
    @GetMapping("")
//    public String showCashPage(HttpSession session, Model model) {
    public String showCashPage(Principal principal, Model model){

        String email = principal.getName();
        //System.out.println("이메일:"+email);
        if (email == null) {
            return "redirect:/members/login";
        }

        /*
        String email = (String) session.getAttribute("email");
        if (email == null) {
            return "redirect:/members/login";
        }
        */

        //Member member = memberService.findByEmail(email);

        Member member = memberRepository.findByEmail(email).orElse(null);


        if (member == null) {
            return "redirect:/members/login";
        }

        Integer cash = member.getCash();
        if (cash == null) {
            cash = 0;
        }

        model.addAttribute("cash", cash);

        return "cash/cash"; // templates/cash/cash.html
    }

    // 캐시 내역 보기
    @GetMapping("/history")
//    public String showCashHistory(HttpSession session, Model model) {
    public String showCashHistory(Principal principal, Model model) {
        //String email = (String) session.getAttribute("email");
        String email = principal.getName();

        //if (email == null) return "redirect:/members/login";
        if (email == null) {
            return "redirect:/members/login";
        }

        //Member member = memberService.findByEmail(email);
        Member member = memberRepository.findByEmail(email).orElse(null);

        if (member == null) return "redirect:/members/login";

        List<CashHistory> historyList = cashService.getCashHistory(member);


        model.addAttribute("cashList", historyList);

        return "cash/cashhistory"; // templates/cash/cashhistory.html
    }



    // ✅ 아임포트 결제 후 검증 및 충전

    @PostMapping("/cash/charge")
//    public ResponseEntity<?> chargeCash(@RequestBody PaymentValidationRequestDto requestDto,
//                                        HttpSession session) {
    public ResponseEntity<?> chargeCash(@RequestBody PaymentValidationRequestDto requestDto,
                                        Principal principal) {
        //String email = (String) session.getAttribute("email");
        String email = principal.getName();
        if (email == null) {
            return ResponseEntity.badRequest().body("로그인이 필요합니다.");
        }

        //Member member = memberService.findByEmail(email);

        Member member = memberRepository.findByEmail(email).orElse(null);

        if (member == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 사용자입니다.");
        }

        try {
            cashService.validateAndCharge(member, requestDto.getImpUid(), requestDto.getAmount());
            return ResponseEntity.ok("캐시 충전 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("결제 검증 실패: " + e.getMessage());
        }
    }



    // ✅ 현재 잔액만 반환 (REST API 용도)
    @GetMapping("/balance")
    public ResponseEntity<?> getCashBalance(HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            return ResponseEntity.badRequest().body("로그인이 필요합니다.");
        }

        //Member member = memberService.findByEmail(email);
        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 사용자입니다.");
        }

        Integer cash = member.getCash();
        return ResponseEntity.ok(cash != null ? cash : 0);
    }




    // ✅ cash.html에서 form 전송으로 충전
    @PostMapping("/charge")
    public String chargeFromForm(@RequestParam("amount") int amount, HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email == null) {
            return "redirect:/members/login";
        }

        //Member member = memberService.findByEmail(email);
        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member != null) {
            cashService.charge(member, amount);
        }

        return "redirect:/cash";
    }









    @PostMapping("/refund")
    @ResponseBody
//    public ResponseEntity<?> requestRefund(@RequestBody RefundRequestDto dto, HttpSession session) {
    public ResponseEntity<?> requestRefund(@RequestBody RefundRequestDto dto, Principal principal) {
        //String email = (String) session.getAttribute("email");
        String email = principal.getName();
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        //Member member = memberService.findByEmail(email);
        Member member = memberRepository.findByEmail(email).orElse(null);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("회원 정보를 찾을 수 없습니다.");
        }

        try {
            cashService.processRefund(member, dto.getHistoryId(), dto.getReason());
            return ResponseEntity.ok("환불 요청이 접수되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("환불 요청 실패: " + e.getMessage());
        }
    }


}
