package com.example.documenttracker.web;

import com.example.documenttracker.domain.User;
import com.example.documenttracker.service.ReminderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/reminders/{id}/read")
    public String markRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        reminderService.markRead(id, user);
        return "redirect:/dashboard#reminders";
    }
}