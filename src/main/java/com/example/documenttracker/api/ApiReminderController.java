package com.example.documenttracker.api;

import com.example.documenttracker.domain.User;
import com.example.documenttracker.service.DocumentService;
import com.example.documenttracker.service.ReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
public class ApiReminderController {

    private final ReminderService reminderService;

    public ApiReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (reminderService.markRead(id, user).isEmpty()) {
            throw new DocumentService.ResourceNotFoundException("Reminder not found");
        }
        return ResponseEntity.noContent().build();
    }
}
