package com.example.documenttracker.api;

import com.example.documenttracker.domain.Reminder;
import com.example.documenttracker.domain.Renewal;
import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;

import static com.example.documenttracker.api.ApiDtos.DocumentView;
import static com.example.documenttracker.api.ApiDtos.ReminderView;
import static com.example.documenttracker.api.ApiDtos.RenewalView;
import static com.example.documenttracker.api.ApiDtos.UserView;

final class ApiMapper {

    private ApiMapper() {
    }

    static UserView user(User user) {
        return new UserView(user.getId(), user.getName(), user.getEmail());
    }

    static DocumentView document(TrackedDocument document) {
        return new DocumentView(document.getId(), document.getTitle(), document.getCategory(),
                document.getCategory().getLabel(), document.getReferenceNumber(), document.getIssueDate(),
                document.getExpiryDate(), document.getNotes(), document.getCreatedAt(),
                document.getDaysUntilExpiry(), document.getStatus().getCssClass(),
                document.getStatus().getLabel());
    }

    static RenewalView renewal(Renewal renewal) {
        return new RenewalView(renewal.getId(), renewal.getPreviousExpiryDate(), renewal.getNewExpiryDate(),
                renewal.getRenewedAt(), renewal.getNotes());
    }

    static ReminderView reminder(Reminder reminder) {
        return new ReminderView(reminder.getId(), document(reminder.getDocument()), reminder.getExpiryDate(),
                reminder.getDaysBefore(), reminder.getCreatedAt(), reminder.getReadAt());
    }
}
