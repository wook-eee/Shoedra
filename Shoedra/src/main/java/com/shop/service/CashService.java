package com.shop.service;

import com.shop.constant.CashHistoryType;
import com.shop.constant.PaymentMethod;
import com.shop.entity.CashHistory;
import com.shop.entity.Member;
import com.shop.repository.CashHistoryRepository;
import com.shop.repository.MemberRepository;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CashService {

    private final MemberRepository memberRepository;
    private final CashHistoryRepository cashHistoryRepository;


    // 캐시 내역 조회
    @Transactional
    public List<CashHistory> getCashHistory(Member member) {
        return cashHistoryRepository.findByMemberOrderByCreatedAtDesc(member);
    }


    // ↓↓↓↓↓↓↓↓↓↓↓↓↓ 충전 ↓↓↓↓↓↓↓↓↓↓↓↓↓



    // ✅ 캐시 충전 (어드민 또는 내부 수동)
    public void charge(Member member, int amount) {
        int currentCash = member.getCash() != null ? member.getCash() : 0;
        int updatedCash = currentCash + amount;
        member.setCash(updatedCash);

        CashHistory history = CashHistory.builder()
                .member(member)
                .amount(amount)
                .cashHistoryType(CashHistoryType.CHARGE)
                .method(PaymentMethod.CHARGE)
                .reason("캐시 충전")
                .createdAt(LocalDateTime.now())
                .refunded(false)
                .build();

        System.out.println("캐쉬:"+member.getCash());

        cashHistoryRepository.save(history);
        memberRepository.save(member);
    }

    // ✅ 캐시 사용
    public void useCash(Member member, int amount) {
        if (member.getCash() < amount) {
            throw new IllegalArgumentException("보유한 캐시가 부족합니다.");
        }

        member.setCash(member.getCash() - amount);
        memberRepository.save(member);

        CashHistory history = CashHistory.builder()
                .member(member)
                .amount(amount)
                .cashHistoryType(CashHistoryType.USE)
                .method(PaymentMethod.INTERNAL)
                .reason("캐시 사용")
                .createdAt(LocalDateTime.now())
                .refunded(false)
                .build();

        cashHistoryRepository.save(history);
    }

    // ✅ 캐시 환불 (내부 테스트용)
    public void refundCash(Member member, int amount) {
        member.setCash(member.getCash() + amount);
        memberRepository.save(member);

        CashHistory history = CashHistory.builder()
                .member(member)
                .amount(amount)
                .cashHistoryType(CashHistoryType.REFUND)
                .method(PaymentMethod.REFUND)
                .reason("캐시 환불")
                .createdAt(LocalDateTime.now())
                .refunded(false)
                .build();

        cashHistoryRepository.save(history);
    }

    // ↓↓↓↓↓↓↓↓↓↓↓↓↓ 결제 ↓↓↓↓↓↓↓↓↓↓↓↓↓

    // ✅ 포트원 결제 충전
    public void validateAndCharge(Member member, String impUid, int amount) {
        PaymentMethod method = PaymentMethod.PORTONE;

        member.setCash(member.getCash() + amount);
        memberRepository.save(member);

        CashHistory history = CashHistory.builder()
                .member(member)
                .amount(amount)
                .cashHistoryType(CashHistoryType.CHARGE)
                .method(method)
                .reason("결제 충전 (impUid: " + impUid + ")")
                .impUid(impUid)
                .createdAt(LocalDateTime.now())
                .refunded(false)
                .build();

        cashHistoryRepository.save(history);
    }



    // ✅ 환불 요청 처리 (포트원 API 연동 포함)
    public void processRefund(Member member, Long historyId, String reason) {
        CashHistory history = cashHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 내역이 존재하지 않습니다."));

        if (!history.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인의 결제 내역만 환불할 수 있습니다.");
        }

        if (Boolean.TRUE.equals(history.getRefunded())) {
            throw new IllegalArgumentException("이미 환불된 내역입니다.");
        }

        String impUid = history.getImpUid();
        if (impUid == null || impUid.isEmpty()) {
            throw new IllegalArgumentException("결제 식별자(impUid)가 없어 환불할 수 없습니다.");
        }

        IamportClient client = new IamportClient(
                "0301567788577323",
                "Mb09VW0jQja1Jfv1udKX5SFZxnVwi8STfE7a8KW9RcykVXjWaZk4wYUTebBqWE771M9Anr3su5dRYHq0");

        CancelData cancelData = new CancelData(impUid, true); // 전체 환불
        cancelData.setReason(reason);

        try {
            IamportResponse<Payment> response = client.cancelPaymentByImpUid(cancelData);
            if (response.getResponse() == null || !"cancelled".equals(response.getResponse().getStatus())) {
                throw new IllegalStateException("PG사 환불 실패");
            }
        } catch (Exception e) {
            throw new RuntimeException("PG사 환불 요청 중 오류 발생: " + e.getMessage());
        }

        // ✅ 기존 CHARGE 이력 수정
        history.setRefunded(true);
        history.setReason(reason + " (환불 완료)");

        // ✅ 캐시 차감
        member.setCash(member.getCash() - history.getAmount());

        // ✅ 환불 기록 추가 (새 이력 생성) - 🔥 추가된 코드
        CashHistory refundRecord = CashHistory.builder()
                .member(member)
                .amount(history.getAmount())
                .cashHistoryType(CashHistoryType.REFUND)
                .method(history.getMethod())
                .reason(reason)
                .impUid(impUid)
                .createdAt(LocalDateTime.now())
                .refunded(true)
                .build();

        // ✅ 저장 순서
        cashHistoryRepository.save(history);        // 기존 CHARGE 이력 수정
        cashHistoryRepository.save(refundRecord);   // REFUND 새 이력 추가
        memberRepository.save(member);              // 캐시 차감
    }



}
