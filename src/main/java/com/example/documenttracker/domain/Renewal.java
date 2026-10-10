package com.example.documenttracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_renewals")
public class Renewal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private TrackedDocument document;

    @Column(name = "previous_expiry_date", nullable = false)
    private LocalDate previousExpiryDate;

    @Column(name = "new_expiry_date", nullable = false)
    private LocalDate newExpiryDate;

    @Column(name = "renewed_at", nullable = false)
    private LocalDateTime renewedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    protected Renewal() {
    }

    public Renewal(TrackedDocument document, LocalDate previousExpiryDate, LocalDate newExpiryDate, String notes) {
        this.document = document;
        this.previousExpiryDate = previousExpiryDate;
        this.newExpiryDate = newExpiryDate;
        this.notes = notes;
    }

    @PrePersist
    void setRenewedAt() {
        if (renewedAt == null) {
            renewedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDate getPreviousExpiryDate() {
        return previousExpiryDate;
    }

    public LocalDate getNewExpiryDate() {
        return newExpiryDate;
    }

    public LocalDateTime getRenewedAt() {
        return renewedAt;
    }

    public String getNotes() {
        return notes;
    }
}