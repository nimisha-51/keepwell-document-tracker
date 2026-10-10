package com.example.documenttracker.api;

import com.example.documenttracker.domain.DocumentCategory;
import com.example.documenttracker.domain.Reminder;
import com.example.documenttracker.domain.TrackedDocument;
import com.example.documenttracker.domain.User;
import com.example.documenttracker.repository.ReminderRepository;
import com.example.documenttracker.repository.RenewalRepository;
import com.example.documenttracker.repository.TrackedDocumentRepository;
import com.example.documenttracker.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:documenttrackerapi;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.reminders.cron=0 0 0 1 1 *",
        "app.cors.allowed-origins=http://localhost:5173"
})
@AutoConfigureMockMvc
class ApiControllerTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrackedDocumentRepository documentRepository;

    @Autowired
    private RenewalRepository renewalRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @BeforeEach
    void clearDatabase() {
        reminderRepository.deleteAll();
        renewalRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void protectsPrivateApiAndRestrictsCorsToConfiguredOrigin() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));

        mockMvc.perform(options("/api/dashboard")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(options("/api/dashboard")
                        .header("Origin", "https://unapproved.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void registersLogsInReturnsCurrentUserAndLogsOut() throws Exception {
        CsrfSession csrf = csrfSession();
        mockMvc.perform(post("/api/auth/register").session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token()).contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"  Keepwell User  ","email":"User@Example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Keepwell User"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
        String storedPassword = userRepository.findByEmailIgnoreCase("user@example.com").orElseThrow().getPassword();
        assertNotEquals(PASSWORD, storedPassword);
        assertTrue(storedPassword.startsWith("$2"));

        mockMvc.perform(post("/api/auth/register").session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token()).contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Another Name","email":"user@example.com","password":"password123"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token()).contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"incorrect123"}
                                """))
                .andExpect(status().isUnauthorized());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token()).contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andReturn();
        MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(authenticatedSession);

        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        CsrfSession authenticatedCsrf = csrfSession(authenticatedSession);
        mockMvc.perform(post("/api/auth/logout").session(authenticatedSession)
                        .header("X-CSRF-TOKEN", authenticatedCsrf.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managesDocumentsAndEnforcesOwnershipAndValidation() throws Exception {
        AccountSession owner = createAndLogin("owner@example.com");
        AccountSession other = createAndLogin("other@example.com");
        String createBody = """
                {"title":"Passport","category":"IDENTIFICATION","referenceNumber":"P-123",
                 "issueDate":"2020-01-01","expiryDate":"%s","notes":"Keep safe"}
                """.formatted(LocalDate.now().plusDays(3));

        MvcResult created = mockMvc.perform(post("/api/documents").session(owner.session())
                        .header("X-CSRF-TOKEN", owner.csrfToken()).contentType(APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Passport"))
                .andExpect(jsonPath("$.statusLabel").value("Due soon"))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andReturn();
        JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        long documentId = createdJson.get("id").asLong();

        mockMvc.perform(get("/api/dashboard").session(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("owner@example.com"))
                .andExpect(jsonPath("$.urgentCount").value(1))
                .andExpect(jsonPath("$.documents[0].id").value(documentId));
        mockMvc.perform(get("/api/documents").session(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Passport"));
        mockMvc.perform(get("/api/documents/{id}", documentId).session(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.title").value("Passport"))
                .andExpect(jsonPath("$.renewals").isEmpty());

        mockMvc.perform(get("/api/documents/{id}", documentId).session(other.session()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/documents/{id}", documentId).session(other.session())
                        .header("X-CSRF-TOKEN", other.csrfToken()).contentType(APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/documents").session(owner.session())
                        .header("X-CSRF-TOKEN", owner.csrfToken()).contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"","category":"IDENTIFICATION","expiryDate":"2026-11-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());
        mockMvc.perform(post("/api/documents").session(owner.session())
                        .header("X-CSRF-TOKEN", owner.csrfToken()).contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Invalid dates","category":"IDENTIFICATION","issueDate":"2026-12-02","expiryDate":"2026-12-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.expiryDate").exists());
        mockMvc.perform(post("/api/documents").session(owner.session()).contentType(APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());

        String updateBody = """
                {"title":"Updated passport","category":"IDENTIFICATION","expiryDate":"%s","notes":"Updated"}
                """.formatted(LocalDate.now().plusDays(45));
        mockMvc.perform(put("/api/documents/{id}", documentId).session(owner.session())
                        .header("X-CSRF-TOKEN", owner.csrfToken()).contentType(APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated passport"));

        mockMvc.perform(post("/api/documents/{id}/renew", documentId).session(owner.session())
                        .header("X-CSRF-TOKEN", owner.csrfToken()).contentType(APPLICATION_JSON)
                        .content("""
                                {"expiryDate":"%s","notes":"Renewed"}
                                """.formatted(LocalDate.now().plusDays(365))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryDate").value(LocalDate.now().plusDays(365).toString()));
        mockMvc.perform(get("/api/documents/{id}/renewals", documentId).session(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].previousExpiryDate").value(LocalDate.now().plusDays(45).toString()))
                .andExpect(jsonPath("$[0].notes").value("Renewed"));

        TrackedDocument trackedDocument = documentRepository.findById(documentId).orElseThrow();
        Reminder reminder = reminderRepository.save(new Reminder(trackedDocument, trackedDocument.getUser(),
                trackedDocument.getExpiryDate(), 0));
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf").session(owner.session())).andReturn();
        String csrfToken = objectMapper.readTree(csrfResult.getResponse().getContentAsString()).get("token").asText();
        mockMvc.perform(post("/api/reminders/{id}/read", reminder.getId()).session(owner.session())
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());
        assertFalse(reminderRepository.findById(reminder.getId()).orElseThrow().getReadAt() == null);

        mockMvc.perform(get("/api/documents/categories").session(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(DocumentCategory.IDENTIFICATION.name()));
    }

    private AccountSession createAndLogin(String email) throws Exception {
        CsrfSession csrf = csrfSession();
        mockMvc.perform(post("/api/auth/register").session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token()).contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApiDtos.RegistrationRequest("Test User", email, PASSWORD))))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token()).contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApiDtos.LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        CsrfSession authenticatedCsrf = csrfSession(session);
        return new AccountSession(session, authenticatedCsrf.token());
    }

    private CsrfSession csrfSession() throws Exception {
        return csrfSession(null);
    }

    private CsrfSession csrfSession(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        if (session != null) request.session(session);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        MockHttpSession csrfSession = (MockHttpSession) result.getRequest().getSession(false);
        return new CsrfSession(body.get("token").asText(), csrfSession);
    }

    private record CsrfSession(String token, MockHttpSession session) {
    }

    private record AccountSession(MockHttpSession session, String csrfToken) {
    }
}
