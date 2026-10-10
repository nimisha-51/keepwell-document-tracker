package com.example.documenttracker.web;

import com.example.documenttracker.domain.DocumentCategory;
import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;
import com.example.documenttracker.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/documents/new")
    public String newDocument(Model model) {
        model.addAttribute("documentForm", new DocumentForm());
        populateFormModel(model, "Add a document", "/documents");
        return "document-form";
    }

    @PostMapping("/documents")
    public String create(@Valid @ModelAttribute("documentForm") DocumentForm form, BindingResult bindingResult,
                         @AuthenticationPrincipal User user, Model model) {
        validateDates(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateFormModel(model, "Add a document", "/documents");
            return "document-form";
        }
        documentService.create(user, form);
        return "redirect:/dashboard?added";
    }

    @GetMapping("/documents/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        TrackedDocument document = documentService.findOwned(id, user);
        model.addAttribute("document", document);
        model.addAttribute("renewals", documentService.findRenewals(id, user));
        return "document-detail";
    }

    @GetMapping("/documents/{id}/edit")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        TrackedDocument document = documentService.findOwned(id, user);
        DocumentForm form = new DocumentForm();
        form.setTitle(document.getTitle());
        form.setCategory(document.getCategory());
        form.setReferenceNumber(document.getReferenceNumber());
        form.setIssueDate(document.getIssueDate());
        form.setExpiryDate(document.getExpiryDate());
        form.setNotes(document.getNotes());
        model.addAttribute("documentForm", form);
        model.addAttribute("documentId", id);
        populateFormModel(model, "Edit document", "/documents/" + id + "/edit");
        return "document-form";
    }

    @PostMapping("/documents/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("documentForm") DocumentForm form,
                         BindingResult bindingResult, @AuthenticationPrincipal User user, Model model) {
        validateDates(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("documentId", id);
            populateFormModel(model, "Edit document", "/documents/" + id + "/edit");
            return "document-form";
        }
        documentService.update(id, user, form);
        return "redirect:/documents/" + id;
    }

    @GetMapping("/documents/{id}/renew")
    public String renewForm(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        TrackedDocument document = documentService.findOwned(id, user);
        RenewalForm form = new RenewalForm();
        form.setExpiryDate(document.getExpiryDate().plusYears(1));
        model.addAttribute("document", document);
        model.addAttribute("renewalForm", form);
        return "renewal-form";
    }

    @PostMapping("/documents/{id}/renew")
    public String renew(@PathVariable Long id, @Valid @ModelAttribute("renewalForm") RenewalForm form,
                        BindingResult bindingResult, @AuthenticationPrincipal User user, Model model) {
        TrackedDocument document = documentService.findOwned(id, user);
        if (form.getExpiryDate() != null && !form.getExpiryDate().isAfter(document.getExpiryDate())) {
            bindingResult.rejectValue("expiryDate", "dateOrder", "New expiry must be after the current expiry date.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("document", document);
            return "renewal-form";
        }
        documentService.renew(id, user, form);
        return "redirect:/documents/" + id + "?renewed";
    }

    private void validateDates(DocumentForm form, BindingResult bindingResult) {
        if (form.getIssueDate() != null && form.getExpiryDate() != null
                && form.getIssueDate().isAfter(form.getExpiryDate())) {
            bindingResult.rejectValue("expiryDate", "dateOrder", "Expiry date must be on or after the issue date.");
        }
    }

    private void populateFormModel(Model model, String pageTitle, String formAction) {
        model.addAttribute("categories", DocumentCategory.values());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
    }
}