package com.shop.service;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmsService {

    private final DefaultMessageService messageService;

    public SmsService() {
        this.messageService = NurigoApp.INSTANCE.initialize(
                "NCSZXJZCGXWDKMZZ",
                "5WRRYTK6IHVHDCIGR0ZZLIVALOAJQNFN",
                "https://api.coolsms.co.kr"
        );
    }

    public void sendSms(String to, String text) {
        Message message = new Message();
        message.setFrom("01031943659");  // 인증된 발신번호
        message.setTo(to);
        message.setText(text);

        try {
            SingleMessageSentResponse response = messageService.sendOne(new SingleMessageSendingRequest(message));
            System.out.println("전송 결과: " + response);
        } catch (Exception e) {
            System.err.println("SMS 전송 실패: " + e.getMessage());
        }
    }
    public void sendSmsToAllMembers(List<String> phoneNumbers, String text) {
        for (String to : phoneNumbers) {
            sendSms(to, text);
        }
    }
}