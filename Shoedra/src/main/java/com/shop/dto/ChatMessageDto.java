package com.shop.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDto {
    private String type; // MESSAGE, JOIN, LEAVE
    private String roomId;
    private String sender;
    private String message;
    private LocalDateTime timestamp;
    private String senderType; // USER, ADMIN, BOT
} 