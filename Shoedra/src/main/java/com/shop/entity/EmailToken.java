package com.shop.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class EmailToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String token;

    private String email;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    // 0은 인증되지 않은 상태, 1은 인증된 상태
    private boolean verified;
}
