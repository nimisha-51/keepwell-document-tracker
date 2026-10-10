package com.example.documenttracker.web;

import com.example.documenttracker.domain.User;
import com.example.documenttracker.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Locale;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registration", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String createAccount(@Valid @ModelAttribute("registration") RegistrationForm form,
                                BindingResult bindingResult, Model model) {
        String email = form.getEmail() == null ? "" : form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!email.isBlank() && userRepository.existsByEmailIgnoreCase(email)) {
            bindingResult.rejectValue("email", "duplicate", "An account with this email already exists.");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }
        userRepository.save(new User(form.getName().trim(), email, passwordEncoder.encode(form.getPassword())));
        return "redirect:/login?registered";
    }
}