package com.newspring.howmuch.settlement;

import java.util.List;

public record ExpenseItem(long amount, Long payerId, List<Long> sharerIds){
    public ExpenseItem {
        if (amount <= 0) {
            throw new IllegalArgumentException("금액은 1원 이상이어야 합니다.");
        }
        if (payerId == null) {
            throw new IllegalArgumentException("결제자는 필수입니다.");
        }
        if (sharerIds == null || sharerIds.isEmpty()) {
            throw new IllegalArgumentException("분담자는 1명 이상이어야 합니다.");
        }
        sharerIds = List.copyOf(sharerIds);
    }
}
