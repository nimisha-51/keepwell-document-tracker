package com.example.documenttracker.api;

import com.example.documenttracker.domain.DocumentCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record UserView(Long id, String name, String email) {
    }

    public record RegistrationRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank @Email @Size(max = 190) String email,
                               @NotBlank String password) {
    }

    public record CsrfView(String token) {
    }

    public record CategoryView(String value, String label) {
    }

    public record DocumentRequest(
            @NotBlank @Size(max = 120) String title,
            @NotNull DocumentCategory category,
            @Size(max = 100) String referenceNumber,
            LocalDate issueDate,
            @NotNull LocalDate expiryDate,
            @Size(max = 4000) String notes) {
    }

    public record RenewalRequest(@NotNull LocalDate expiryDate, @Size(max = 4000) String notes) {
    }

    public record DocumentView(Long id, String title, DocumentCategory category, String categoryLabel,
                               String referenceNumber, LocalDate issueDate, LocalDate expiryDate, String notes,
                               LocalDateTime createdAt, long daysUntilExpiry, String status, String statusLabel) {
    }

    public record RenewalView(Long id, LocalDate previousExpiryDate, LocalDate newExpiryDate,
                              LocalDateTime renewedAt, String notes) {
    }

    public record DocumentDetailView(DocumentView document, List<RenewalView> renewals) {
    }

    public record ReminderView(Long id, DocumentView document, LocalDate expiryDate, int daysBefore,
                               LocalDateTime createdAt, LocalDateTime readAt) {
    }

    public record DashboardView(LocalDate today, UserView user, List<DocumentView> documents,
                                long expiredCount, long urgentCount, long upcomingCount, long currentCount,
                                long dueWithin30DaysCount, List<ReminderView> reminders, long unreadReminders) {
    }

    public record ApiError(String error, Map<String, String> fields) {
    }
}
