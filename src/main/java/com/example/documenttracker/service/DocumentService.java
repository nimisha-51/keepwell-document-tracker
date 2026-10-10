package com.example.documenttracker.service;

import com.example.documenttracker.domain.Renewal;
import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;
import com.example.documenttracker.repository.RenewalRepository;
import com.example.documenttracker.repository.TrackedDocumentRepository;
import com.example.documenttracker.web.DocumentForm;
import com.example.documenttracker.web.RenewalForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DocumentService {

    private final TrackedDocumentRepository documentRepository;
    private final RenewalRepository renewalRepository;

    public DocumentService(TrackedDocumentRepository documentRepository, RenewalRepository renewalRepository) {
        this.documentRepository = documentRepository;
        this.renewalRepository = renewalRepository;
    }

    @Transactional(readOnly = true)
    public List<TrackedDocument> findAll(User user) {
        return documentRepository.findAllByUserOrderByExpiryDateAsc(user);
    }

    @Transactional(readOnly = true)
    public TrackedDocument findOwned(Long id, User user) {
        return documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    public TrackedDocument create(User user, DocumentForm form) {
        return documentRepository.save(new TrackedDocument(user, clean(form.getTitle()), form.getCategory(),
                clean(form.getReferenceNumber()), form.getIssueDate(), form.getExpiryDate(), clean(form.getNotes())));
    }

    public TrackedDocument update(Long id, User user, DocumentForm form) {
        TrackedDocument document = findOwned(id, user);
        document.update(clean(form.getTitle()), form.getCategory(), clean(form.getReferenceNumber()),
                form.getIssueDate(), form.getExpiryDate(), clean(form.getNotes()));
        return documentRepository.save(document);
    }

    public void renew(Long id, User user, RenewalForm form) {
        TrackedDocument document = findOwned(id, user);
        LocalDate newExpiryDate = form.getExpiryDate();
        if (newExpiryDate == null || !newExpiryDate.isAfter(document.getExpiryDate())) {
            throw new IllegalArgumentException("The new expiry date must be after the current expiry date.");
        }
        renewalRepository.save(new Renewal(document, document.getExpiryDate(), newExpiryDate, clean(form.getNotes())));
        document.renew(newExpiryDate);
    }

    @Transactional(readOnly = true)
    public List<Renewal> findRenewals(Long id, User user) {
        TrackedDocument document = findOwned(id, user);
        return renewalRepository.findAllByDocumentOrderByRenewedAtDesc(document);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}