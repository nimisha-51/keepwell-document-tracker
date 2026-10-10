package com.example.documenttracker.repository;

import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrackedDocumentRepository extends JpaRepository<TrackedDocument, Long> {
    List<TrackedDocument> findAllByUserOrderByExpiryDateAsc(User user);
    Optional<TrackedDocument> findByIdAndUser(Long id, User user);
    List<TrackedDocument> findAllByExpiryDate(LocalDate expiryDate);
}