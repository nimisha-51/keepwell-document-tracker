package com.example.documenttracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "tracked_documents")
public class TrackedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentCategory category;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TrackedDocument() {
    }

    public TrackedDocument(User user, String title, DocumentCategory category, String referenceNumber,
                           LocalDate issueDate, LocalDate expiryDate, String notes) {
        this.user = user;
        this.title = title;
        this.category = category;
        this.referenceNumber = referenceNumber;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.notes = notes;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public long getDaysUntilExpiry() {
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public DocumentStatus getStatus() {
        return DocumentStatus.fromExpiryDate(expiryDate, LocalDate.now());
    }

    public void update(String title, DocumentCategory category, String referenceNumber,
                       LocalDate issueDate, LocalDate expiryDate, String notes) {
        this.title = title;
        this.category = category;
        this.referenceNumber = referenceNumber;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.notes = notes;
    }

    public void renew(LocalDate newExpiryDate) {
        this.expiryDate = newExpiryDate;
    }
}