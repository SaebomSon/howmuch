package com.newspring.howmuch.settlement;

public record ParticipantBalance(Long participantId, long paid, long owed, long balance) {
}
