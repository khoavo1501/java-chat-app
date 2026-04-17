package com.chat.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chat.entity.UserAccount;
import com.chat.repository.UserAccountRepository;

@Configuration
public class BootstrapDataConfig {

    @Bean
    public ApplicationRunner bootstrapAdmin(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            org.springframework.core.env.Environment environment) {

        return args -> {
            String adminUsername = environment.getProperty("app.admin.username", "admin");
            String adminPassword = environment.getProperty("app.admin.password", "admin123");

            if (!userAccountRepository.existsByUsername(adminUsername)) {
                UserAccount admin = new UserAccount();
                admin.setUsername(adminUsername);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setOnline(false);
                admin.setTheme("midnight");
                admin.addRole("USER");
                admin.addRole("ADMIN");
                admin.touchForCreate();
                userAccountRepository.save(admin);
            }
        };
    }
}