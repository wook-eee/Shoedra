package com.shop.dto;

import com.shop.constant.CashHistoryType;
import com.shop.entity.CashHistory;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashHistoryDto {

    private int amount;
    private CashHistoryType type;
    private LocalDateTime createdAt;






    // ✅ CashHistory → DTO 변환용 메서드
    public static CashHistoryDto fromEntity(CashHistory history) {
        return new CashHistoryDto(
                history.getAmount(),
                history.getCashHistoryType(),
                history.getCreatedAt()
        );
    }
}
