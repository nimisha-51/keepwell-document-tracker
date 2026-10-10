package com.example.documenttracker.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum DocumentStatus {
    EXPIRED("Expired", "status-expired"),
    URGENT("Due soon", "status-urgent"),
    UPCOMING("Upcoming", "status-upcoming"),
    CURRENT("Current", "status-current");

    private final String label;
    private final String cssClass;

    DocumentStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static DocumentStatus fromExpiryDate(LocalDate expiryDate, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, expiryDate);
        if (days < 0) {
            return EXPIRED;
        }
        if (days <= 7) {
            return URGENT;
        }
        if (days <= 30) {
            return UPCOMING;
        }
        return CURRENT;
    }
}