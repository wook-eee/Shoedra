package com.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
@ToString
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @Column(name = "chat_room_id", insertable = false, updatable = false)
    private Long chatRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    @Column(name = "sender_id", insertable = false, updatable = false)
    private Long senderId;

    private String message;

    private String messageType; // "MESSAGE", "JOIN", "LEAVE"

    private String senderType; // "USER", "ADMIN", "BOT"

    private LocalDateTime sentTime;

    @PrePersist
    protected void onCreate() {
        sentTime = LocalDateTime.now();
    }
} 