package com.example.documenttracker.api;

import com.example.documenttracker.domain.DocumentStatus;
import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;
import com.example.documenttracker.service.DocumentService;
import com.example.documenttracker.service.ReminderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static com.example.documenttracker.api.ApiDtos.*;

@RestController
@RequestMapping("/api/dashboard")
public class ApiDashboardController {

    private final DocumentService documentService;
    private final ReminderService reminderService;

    public ApiDashboardController(DocumentService documentService, ReminderService reminderService) {
        this.documentService = documentService;
        this.reminderService = reminderService;
    }

    @GetMapping
    public DashboardView dashboard(@AuthenticationPrincipal User user) {
        List<TrackedDocument> documents = documentService.findAll(user);
        List<DocumentView> documentViews = documents.stream().map(ApiMapper::document).toList();
        long expired = count(documents, DocumentStatus.EXPIRED);
        long urgent = count(documents, DocumentStatus.URGENT);
        long upcoming = count(documents, DocumentStatus.UPCOMING);
        long current = count(documents, DocumentStatus.CURRENT);
        return new DashboardView(LocalDate.now(), ApiMapper.user(user), documentViews, expired, urgent, upcoming,
                current, urgent + upcoming, reminderService.recentFor(user).stream().map(ApiMapper::reminder).toList(),
                reminderService.unreadCountFor(user));
    }

    private long count(List<TrackedDocument> documents, DocumentStatus status) {
        return documents.stream().filter(document -> document.getStatus() == status).count();
    }
}
