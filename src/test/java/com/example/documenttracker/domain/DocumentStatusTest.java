package com.example.documenttracker.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentStatusTest {

    private final LocalDate today = LocalDate.of(2026, 10, 5);

    @Test
    void marksPastDatesAsExpired() {
        assertEquals(DocumentStatus.EXPIRED, DocumentStatus.fromExpiryDate(today.minusDays(1), today));
    }

    @Test
    void marksTodayThroughSevenDaysAsUrgent() {
        assertEquals(DocumentStatus.URGENT, DocumentStatus.fromExpiryDate(today, today));
        assertEquals(DocumentStatus.URGENT, DocumentStatus.fromExpiryDate(today.plusDays(7), today));
    }

    @Test
    void marksEightThroughThirtyDaysAsUpcoming() {
        assertEquals(DocumentStatus.UPCOMING, DocumentStatus.fromExpiryDate(today.plusDays(8), today));
        assertEquals(DocumentStatus.UPCOMING, DocumentStatus.fromExpiryDate(today.plusDays(30), today));
    }

    @Test
    void marksDatesBeyondThirtyDaysAsCurrent() {
        assertEquals(DocumentStatus.CURRENT, DocumentStatus.fromExpiryDate(today.plusDays(31), today));
    }
}