package com.newspring.howmuch.settlement;

public record TransferItem(Long senderId, Long receiverId, long amount) {
    public TransferItem {
        if (amount <= 0) {
            throw new IllegalArgumentException("금액은 1원 이상이어야 합니다.");
        }
        if (senderId == null || receiverId == null) {
            throw new IllegalArgumentException("보낸 사람과 받은 사람은 필수입니다.");
        }
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("보낸 사람과 받은 사람이 같을 수 없습니다.");
        }
    }
}
