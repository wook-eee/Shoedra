package com.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "chat_room")
@Getter
@Setter
@ToString
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String roomName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Member admin;

    private String chatType; // "1:1" or "CHATBOT"
    
    private String status; // "WAITING", "IN_PROGRESS", "COMPLETED"

    @Column(name = "hidden")
    private boolean hidden = false;
} 