package com.newspring.howmuch.settlement;

import java.util.List;

public record SettlementResult(long totalAmount, List<ParticipantBalance> balances, List<Payment> payments) {
}
