package com.example.documenttracker.web;

import com.example.documenttracker.domain.DocumentStatus;
import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;
import com.example.documenttracker.service.DocumentService;
import com.example.documenttracker.service.ReminderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {

    private final DocumentService documentService;
    private final ReminderService reminderService;

    public DashboardController(DocumentService documentService, ReminderService reminderService) {
        this.documentService = documentService;
        this.reminderService = reminderService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User user, Model model) {
        List<TrackedDocument> documents = documentService.findAll(user);
        model.addAttribute("user", user);
        model.addAttribute("documents", documents);
        model.addAttribute("reminders", reminderService.recentFor(user));
        model.addAttribute("unreadReminders", reminderService.unreadCountFor(user));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("expiredCount", count(documents, DocumentStatus.EXPIRED));
        model.addAttribute("urgentCount", count(documents, DocumentStatus.URGENT));
        model.addAttribute("upcomingCount", count(documents, DocumentStatus.UPCOMING));
        model.addAttribute("currentCount", count(documents, DocumentStatus.CURRENT));
        return "dashboard";
    }

    private long count(List<TrackedDocument> documents, DocumentStatus status) {
        return documents.stream().filter(document -> document.getStatus() == status).count();
    }
}