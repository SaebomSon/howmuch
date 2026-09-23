package com.newspring.howmuch.transfer;

import com.newspring.howmuch.participant.Participant;
import com.newspring.howmuch.trip.Trip;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Participant sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Participant receiver;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 100)
    private String memo;

    public Transfer(Trip trip, Participant sender, Participant receiver, Long amount, String memo) {
        this.trip = trip;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.memo = memo;
    }
}
