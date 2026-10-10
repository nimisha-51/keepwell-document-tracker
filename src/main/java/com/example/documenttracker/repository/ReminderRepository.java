package com.example.documenttracker.repository;

import com.example.documenttracker.domain.Reminder;
import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    boolean existsByDocumentAndExpiryDateAndLeadDays(TrackedDocument document, LocalDate expiryDate, int leadDays);
    @EntityGraph(attributePaths = "document")
    List<Reminder> findTop10ByUserOrderByCreatedAtDesc(User user);
    Optional<Reminder> findByIdAndUser(Long id, User user);
    long countByUserAndReadAtIsNull(User user);
}