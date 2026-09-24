package com.newspring.howmuch.settlement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementCalculatorTest {
    private static final Long ME = 1L;
    private static final Long FRIEND = 2L;

    private final SettlementCalculator calculator = new SettlementCalculator();

    @Test
    @DisplayName("2인 여행: 현금 이체를 반영해 최종 송금액을 계산한다.")
    void twoPersonTrip() {
        SettlementResult result = calculator.calculate(
                List.of(ME, FRIEND),
                List.of(
                        new ExpenseItem(190_000, FRIEND, List.of(ME, FRIEND)),
                        new ExpenseItem(119_333, FRIEND, List.of(ME, FRIEND)),
                        new ExpenseItem(979_895, ME, List.of(ME, FRIEND))
                ),
                List.of(new TransferItem(FRIEND, ME, 310_000))
        );

        assertThat(result.totalAmount()).isEqualTo(1_289_228);
        assertThat(result.balances()).extracting(ParticipantBalance::owed)
                .containsOnly(644_614L);
        assertThat(result.payments())
                .containsExactlyInAnyOrder(new Payment(FRIEND, ME, 25_281));
    }

    @Test
    @DisplayName("나누어 떨어지지 않아도 잔액의 합은 0이 된다.")
    void indivisiblaAmount() {
        SettlementResult result = calculator.calculate(
                List.of(1L, 2L, 3L),
                List.of(new ExpenseItem(100, 1L, List.of(1L, 2L, 3L))),
                List.of()
        );
        assertThat(result.balances()).extracting(ParticipantBalance::balance)
                .containsExactlyInAnyOrder(67L, -33L, -34L);
        long sum = result.balances().stream().mapToLong(ParticipantBalance::balance).sum();
        assertThat(sum).isZero();
    }

    @Test
    @DisplayName("이미 정산이 끝났으면 송금 안내가 없다.")
    void alreadySettled() {
        SettlementResult result = calculator.calculate(
                List.of(ME, FRIEND),
                List.of(new ExpenseItem(10_000, ME, List.of(ME, FRIEND))),
                List.of(new TransferItem(FRIEND, ME, 5_000))
        );
        assertThat(result.payments()).isEmpty();
    }

    @Test
    @DisplayName("참가자 목록에 없는 사람이 결제자면 예외가 발생한다.")
    void unknownPayer() {
        assertThatThrownBy(() -> calculator.calculate(
                List.of(ME, FRIEND),
                List.of(new ExpenseItem(10_000, 99L, List.of(ME, FRIEND))),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
