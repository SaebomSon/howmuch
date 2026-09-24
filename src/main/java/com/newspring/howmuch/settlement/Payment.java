package com.newspring.howmuch.settlement;

public record Payment(Long fromId, Long toId, long amount) {
}
