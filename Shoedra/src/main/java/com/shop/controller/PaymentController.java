package com.shop.controller;

import com.shop.dto.PaymentValidationRequestDto;
//import com.shop.service.OrderService;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

    //private final OrderService orderService;
    private final IamportClient iamportClient;

    // ✅ 포트원 결제 성공 콜백 처리 (기존)
    @PostMapping("/portone/success")
    public ResponseEntity<?> handlePortOnePayment(
            @RequestParam("imp_uid") String impUid,
            @RequestParam("merchant_uid") String merchantUid
    ) {
        try {
            IamportResponse<Payment> response = iamportClient.paymentByImpUid(impUid);
            if (response.getResponse() != null && "paid".equals(response.getResponse().getStatus())) {
                //orderService.updatePaymentSuccess(merchantUid, impUid, response.getResponse());
                return ResponseEntity.ok("결제 성공");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("결제 실패 또는 미결제 상태입니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("결제 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // ✅ 포트원 결제 실패 콜백 처리
    @GetMapping("/fail")
    public ResponseEntity<?> paymentFail(@RequestParam(value = "message", required = false) String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("결제 실패: " + (message != null ? message : "알 수 없는 오류"));
    }

    // ✅ 프론트에서 결제 완료 후 검증 요청 (imp_uid와 금액 일치 여부)
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentValidationRequestDto request) {
        try {
            IamportResponse<Payment> response = iamportClient.paymentByImpUid(request.getImpUid());

            if (response.getResponse() != null) {
                Payment payment = response.getResponse();

                if ("paid".equals(payment.getStatus()) && payment.getAmount().intValue() == request.getAmount()) {
                    return ResponseEntity.ok().body("{\"success\": true}");
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("{\"success\": false, \"message\": \"결제 금액 불일치 또는 미결제 상태\"}");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"success\": false, \"message\": \"결제 정보를 찾을 수 없습니다\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\": false, \"message\": \"검증 중 오류 발생: " + e.getMessage() + "\"}");
        }
    }


}
