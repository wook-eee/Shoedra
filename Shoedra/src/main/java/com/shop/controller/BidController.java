package com.shop.controller;

import com.shop.dto.BidRequestDto;
import com.shop.entity.Member;
import com.shop.repository.BidAuctionRepository;
import com.shop.repository.MemberRepository;
import com.shop.service.BidService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
public class BidController {
    private final BidService bidService;
    private final MemberRepository memberRepository;
    private final BidAuctionRepository bidAuctionRepository;

    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> auctionEmitters = new ConcurrentHashMap<>();


    @PostMapping("/auction/bid")
    public ResponseEntity<?> placeBid(@RequestBody @Valid BidRequestDto bidRequestDto,
                                      BindingResult bindingResult,
                                      Principal principal){
        if(bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                sb.append(fieldError.getDefaultMessage());
            }

            return new ResponseEntity<String>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        String email = principal.getName();

        try {

            Member findMember = memberRepository.findByEmail(email).orElse(null);


            if(findMember == null){
                throw new UsernameNotFoundException(principal.getName());
            }


            int currentPrice = bidService.addBid(findMember.getId(), bidRequestDto.getAuctionId(), bidRequestDto.getBidPrice());

            return new ResponseEntity<Integer>(currentPrice, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            // 입찰가가 낮거나 기타 서비스에서 발생한 예외 처리
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러가 발생했습니다.");
        }

    }

//    @GetMapping("/sse/list")
//    @PreAuthorize("hasRole('ADMIN')")  // 관리자만 접근 가능 (선택 사항)
//    public ResponseEntity<Set<String>> getConnectedUsers() {
//        return ResponseEntity.ok(auctionEmitters.get(0).keySet());
//    }
}
