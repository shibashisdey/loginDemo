package com.example.login.config;
import com.example.login.model.User;
import com.example.login.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@example.com"; // Set your admin email here
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User adminUser = new User();
            adminUser.setName("Admin");  // <-- Add this, mandatory field
            adminUser.setEmail(adminEmail);
            adminUser.setPassword(passwordEncoder.encode("adminpassword")); // Change to a strong password
            adminUser.setRoles(Set.of("ROLE_ADMIN"));
            adminUser.setEnabled(true);
            // You can also set other optional fields like gender, phone, dob, height if needed

            userRepository.save(adminUser);
            System.out.println("Default admin user created: " + adminEmail);
        }
    }
}

