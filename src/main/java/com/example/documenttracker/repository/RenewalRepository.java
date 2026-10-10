package com.example.documenttracker.repository;

import com.example.documenttracker.domain.Renewal;
import com.example.documenttracker.domain.TrackedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RenewalRepository extends JpaRepository<Renewal, Long> {
    List<Renewal> findAllByDocumentOrderByRenewedAtDesc(TrackedDocument document);
}