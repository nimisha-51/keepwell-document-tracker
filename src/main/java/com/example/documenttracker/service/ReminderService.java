package com.example.documenttracker.service;

import com.example.documenttracker.domain.Reminder;
import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;
import com.example.documenttracker.repository.ReminderRepository;
import com.example.documenttracker.repository.TrackedDocumentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
public class ReminderService {

    private static final List<Integer> REMINDER_DAYS = IntStream.rangeClosed(0, 10).boxed().toList();

    private final TrackedDocumentRepository documentRepository;
    private final ReminderRepository reminderRepository;

    public ReminderService(TrackedDocumentRepository documentRepository, ReminderRepository reminderRepository) {
        this.documentRepository = documentRepository;
        this.reminderRepository = reminderRepository;
    }

    @Scheduled(cron = "${app.reminders.cron:0 0 8 * * *}")
    @Transactional
    public void createDailyReminders() {
        LocalDate today = LocalDate.now();
        for (int daysBefore : REMINDER_DAYS) {
            LocalDate expiryDate = today.plusDays(daysBefore);
            for (TrackedDocument document : documentRepository.findAllByExpiryDate(expiryDate)) {
                if (!reminderRepository.existsByDocumentAndExpiryDateAndLeadDays(document, expiryDate, daysBefore)) {
                    reminderRepository.save(new Reminder(document, document.getUser(), expiryDate, daysBefore));
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Reminder> recentFor(User user) {
        return reminderRepository.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long unreadCountFor(User user) {
        return reminderRepository.countByUserAndReadAtIsNull(user);
    }

    @Transactional
    public Optional<Reminder> markRead(Long id, User user) {
        Optional<Reminder> reminder = reminderRepository.findByIdAndUser(id, user);
        reminder.ifPresent(Reminder::markRead);
        return reminder;
    }
}