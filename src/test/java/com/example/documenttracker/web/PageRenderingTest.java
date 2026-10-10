package com.example.documenttracker.web;

import com.example.documenttracker.domain.DocumentCategory;
import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;
import com.example.documenttracker.repository.ReminderRepository;
import com.example.documenttracker.repository.RenewalRepository;
import com.example.documenttracker.repository.TrackedDocumentRepository;
import com.example.documenttracker.repository.UserRepository;
import com.example.documenttracker.service.ReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:documenttracker;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.reminders.cron=0 0 0 1 1 *"
})
@AutoConfigureMockMvc
class PageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrackedDocumentRepository documentRepository;

    @Autowired
    private RenewalRepository renewalRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

        @Autowired
        private ReminderService reminderService;

    private User account;
    private TrackedDocument document;

    @BeforeEach
    void setUp() {
        reminderRepository.deleteAll();
        renewalRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
        account = userRepository.save(new User("Sample User", "sample@example.com", passwordEncoder.encode("password123")));
        document = documentRepository.save(new TrackedDocument(account, "Passport", DocumentCategory.IDENTIFICATION,
                "P-123", LocalDate.of(2020, 1, 1), LocalDate.now().plusDays(20), "Renewal notes"));
    }

    @Test
    void publicAccountPagesRender() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
        mockMvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("register"));
    }

    @Test
    void signedInDocumentPagesRender() throws Exception {
        mockMvc.perform(get("/dashboard").with(user(account)))
                .andExpect(status().isOk()).andExpect(view().name("dashboard"));
        mockMvc.perform(get("/documents/new").with(user(account)))
                .andExpect(status().isOk()).andExpect(view().name("document-form"));
        mockMvc.perform(get("/documents/{id}", document.getId()).with(user(account)))
                .andExpect(status().isOk()).andExpect(view().name("document-detail"));
        mockMvc.perform(get("/documents/{id}/edit", document.getId()).with(user(account)))
                .andExpect(status().isOk()).andExpect(view().name("document-form"));
        mockMvc.perform(get("/documents/{id}/renew", document.getId()).with(user(account)))
                .andExpect(status().isOk()).andExpect(view().name("renewal-form"));
    }

        @Test
        void usersCannotViewAnotherAccountsDocument() throws Exception {
                User other = userRepository.save(new User("Other User", "other@example.com", passwordEncoder.encode("password123")));

                mockMvc.perform(get("/documents/{id}", document.getId()).with(user(other)))
                                .andExpect(status().isNotFound()).andExpect(view().name("not-found"));
        }

        @Test
        void createsDailyRemindersForTenDayCountdownAndDoesNotDuplicateThem() {
                for (int daysUntilExpiry = 0; daysUntilExpiry <= 10; daysUntilExpiry++) {
                        documentRepository.save(new TrackedDocument(account, "Insurance " + daysUntilExpiry,
                                        DocumentCategory.INSURANCE, null, null, LocalDate.now().plusDays(daysUntilExpiry), null));
                }

                reminderService.createDailyReminders();
                reminderService.createDailyReminders();

                assertEquals(11, reminderService.unreadCountFor(account));
        }
}