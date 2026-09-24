package com.newspring.howmuch.settlement;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

public class SettlementCalculator {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    public SettlementResult calculate(List<Long> participantIds,
                                      List<ExpenseItem> expenses,
                                      List<TransferItem> transfers) {

        Map<Long, Long> paid = new LinkedHashMap<>();
        Map<Long, BigDecimal> owed = new LinkedHashMap<>();
        Map<Long, Long> netTransfers = new LinkedHashMap<>();
        for (Long id: participantIds) {
            paid.put(id, 0L);
            owed.put(id, BigDecimal.ZERO);
            netTransfers.put(id, 0L);
        }

        long totalAmount = 0L;
        for (ExpenseItem expense: expenses) {
            validateKnown(paid, expense.payerId());
            expense.sharerIds().forEach(id -> validateKnown(paid, id));

            totalAmount += expense.amount();
            paid.merge(expense.payerId(), expense.amount(), Long::sum);

            BigDecimal share = BigDecimal.valueOf(expense.amount())
                    .divide(BigDecimal.valueOf(expense.sharerIds().size()), MATH_CONTEXT);
            for (Long sharerId: expense.sharerIds()) {
                owed.merge(sharerId, share, BigDecimal::add);
            }
        }

        for (TransferItem transfer: transfers) {
            validateKnown(paid, transfer.senderId());
            validateKnown(paid, transfer.receiverId());
            netTransfers.merge(transfer.senderId(), transfer.amount(), Long::sum);
            netTransfers.merge(transfer.receiverId(), -transfer.amount(), Long::sum);
        }

        Map<Long, BigDecimal> rawBalance = new LinkedHashMap<>();
        for (Long id : participantIds) {
            rawBalance.put(id, BigDecimal.valueOf(paid.get(id))
                    .subtract(owed.get(id))
                    .add(BigDecimal.valueOf(netTransfers.get(id))));
        }

        Map<Long, Long> rounded = roundKeepingZeroSum(rawBalance);

        List<ParticipantBalance> balances = participantIds.stream()
                .map(id -> new ParticipantBalance(
                        id,
                        paid.get(id),
                        owed.get(id).setScale(0, RoundingMode.HALF_UP).longValueExact(),
                        rounded.get(id)))
                .toList();

        return new SettlementResult(totalAmount, balances, matchPayments(rounded));
    }

    private void validateKnown(Map<Long, Long> participants, Long id){
        if (!participants.containsKey(id)) {
            throw new IllegalArgumentException("참가자 목록에 없는 사람입니다: " + id);
        }
    }

    private Map<Long, Long> roundKeepingZeroSum(Map<Long, BigDecimal> rawBalance) {
        Map<Long, Long> rounded = new LinkedHashMap<>();
        long sum = 0L;
        for (Map.Entry<Long, BigDecimal> entry: rawBalance.entrySet()) {
            long floor = entry.getValue().setScale(0, RoundingMode.FLOOR).longValueExact();
            rounded.put(entry.getKey(), floor);
            sum += floor;
        }
        long remainder = -sum;

        List<Long> orderedIdes = new ArrayList<>(rawBalance.keySet());
        orderedIdes.sort(Comparator.comparing((Long id) -> rawBalance.get(id)
                        .subtract(new BigDecimal(rounded.get(id))))
                .reversed()
                .thenComparing(Comparator.naturalOrder()));

        for (int i = 0; i < remainder; i++) {
            rounded.merge(orderedIdes.get(i % orderedIdes.size()), 1L, Long::sum);
        }
        return rounded;
    }

    private List<Payment> matchPayments(Map<Long, Long> balances) {
        Deque<ParticipantBalance> creditors = new ArrayDeque<>();
        Deque<ParticipantBalance> debtors = new ArrayDeque<>();

        List<Map.Entry<Long, Long>> entries = new ArrayList<>(balances.entrySet());
        entries.stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> creditors.add(new ParticipantBalance(e.getKey(), 0, 0, e.getValue())));
        entries.stream()
                .filter(e -> e.getValue() < 0)
                .sorted(Map.Entry.<Long, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> debtors.add(new ParticipantBalance(e.getKey(), 0, 0, -e.getValue())));

        List<Payment> payments = new ArrayList<>();
        ParticipantBalance creditor = creditors.poll();
        ParticipantBalance debtor = debtors.poll();
        while (creditor != null && debtor != null) {
            long amount = Math.min(creditor.balance(), debtor.balance());
            payments.add(new Payment(debtor.participantId(), creditor.participantId(), amount));

            long creditorLeft = creditor.balance() - amount;
            long debtorLeft = debtor.balance() - amount;
            creditor = creditorLeft == 0 ? creditors.poll()
                    : new ParticipantBalance(creditor.participantId(), 0, 0, creditorLeft);
            debtor = debtorLeft == 0 ? debtors.poll()
                    : new ParticipantBalance(debtor.participantId(), 0, 0, debtorLeft);
        }
        return payments;
    }
}
