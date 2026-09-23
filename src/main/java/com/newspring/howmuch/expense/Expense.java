package com.newspring.howmuch.expense;

import com.newspring.howmuch.participant.Participant;
import com.newspring.howmuch.trip.Trip;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false)
    private Long amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id", nullable = false)
    private Participant payer;

    private LocalDate spentOn;

    @Column(length = 100)
    private String memo;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseShare> shares = new ArrayList<>();

    public Expense(Trip trip, String title, Long amount, Participant payer, LocalDate spentOn, String memo) {
        this.trip = trip;
        this.title = title;
        this.amount = amount;
        this.payer = payer;
        this.spentOn = spentOn;
        this.memo = memo;
    }

    public void addShare(Participant participant){
        shares.add(new ExpenseShare(this, participant));
    }
}
