package com.example.documenttracker.api;

import com.example.documenttracker.domain.DocumentCategory;
import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;
import com.example.documenttracker.service.DocumentService;
import com.example.documenttracker.web.DocumentForm;
import com.example.documenttracker.web.RenewalForm;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.example.documenttracker.api.ApiDtos.*;

@RestController
@RequestMapping("/api/documents")
public class ApiDocumentController {

    private final DocumentService documentService;

    public ApiDocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/categories")
    public List<CategoryView> categories() {
        return Arrays.stream(DocumentCategory.values())
                .map(category -> new CategoryView(category.name(), category.getLabel()))
                .toList();
    }

    @GetMapping
    public List<DocumentView> list(@AuthenticationPrincipal User user) {
        return documentService.findAll(user).stream().map(ApiMapper::document).toList();
    }

    @PostMapping
    public ResponseEntity<DocumentView> create(@Valid @RequestBody DocumentRequest request,
                                               @AuthenticationPrincipal User user) {
        validateDates(request.issueDate(), request.expiryDate());
        TrackedDocument document = documentService.create(user, toForm(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(document.getId()).toUri();
        return ResponseEntity.created(location).body(ApiMapper.document(document));
    }

    @GetMapping("/{id}")
    public DocumentDetailView detail(@PathVariable Long id, @AuthenticationPrincipal User user) {
        TrackedDocument document = documentService.findOwned(id, user);
        return new DocumentDetailView(ApiMapper.document(document),
                documentService.findRenewals(id, user).stream().map(ApiMapper::renewal).toList());
    }

    @PutMapping("/{id}")
    public DocumentView update(@PathVariable Long id, @Valid @RequestBody DocumentRequest request,
                               @AuthenticationPrincipal User user) {
        validateDates(request.issueDate(), request.expiryDate());
        return ApiMapper.document(documentService.update(id, user, toForm(request)));
    }

    @PostMapping("/{id}/renew")
    public DocumentView renew(@PathVariable Long id, @Valid @RequestBody RenewalRequest request,
                              @AuthenticationPrincipal User user) {
        TrackedDocument document = documentService.findOwned(id, user);
        if (!request.expiryDate().isAfter(document.getExpiryDate())) {
            throw new ApiValidationException("expiryDate", "New expiry must be after the current expiry date.");
        }
        RenewalForm form = new RenewalForm();
        form.setExpiryDate(request.expiryDate());
        form.setNotes(request.notes());
        documentService.renew(id, user, form);
        return ApiMapper.document(documentService.findOwned(id, user));
    }

    @GetMapping("/{id}/renewals")
    public List<RenewalView> renewals(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return documentService.findRenewals(id, user).stream().map(ApiMapper::renewal).toList();
    }

    private DocumentForm toForm(DocumentRequest request) {
        DocumentForm form = new DocumentForm();
        form.setTitle(request.title());
        form.setCategory(request.category());
        form.setReferenceNumber(request.referenceNumber());
        form.setIssueDate(request.issueDate());
        form.setExpiryDate(request.expiryDate());
        form.setNotes(request.notes());
        return form;
    }

    private void validateDates(LocalDate issueDate, LocalDate expiryDate) {
        if (issueDate != null && expiryDate != null && issueDate.isAfter(expiryDate)) {
            throw new ApiValidationException("expiryDate", "Expiry date must be on or after the issue date.");
        }
    }
}
