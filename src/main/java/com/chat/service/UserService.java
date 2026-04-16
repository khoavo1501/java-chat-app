package com.chat.service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chat.dto.UserPresenceResponse;
import com.chat.entity.UserAccount;
import com.chat.repository.UserAccountRepository;

@Service
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,30}$");

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(String rawUsername, String rawPassword) {
        String username = normalizeUsername(rawUsername);
        String password = normalizePassword(rawPassword);

        validateRegistrationInput(username, password);

        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username da ton tai.");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setOnline(false);
        return userAccountRepository.save(user);
    }

    public List<UserPresenceResponse> getAllPresence() {
        return userAccountRepository.findAllByOrderByUsernameAsc()
                .stream()
                .map(user -> new UserPresenceResponse(user.getUsername(), user.isOnline()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateOnline(String username, boolean online) {
        userAccountRepository.findByUsername(username).ifPresent(user -> {
            if (user.isOnline() != online) {
                user.setOnline(online);
                userAccountRepository.save(user);
            }
        });
    }

    public boolean existsByUsername(String username) {
        return userAccountRepository.existsByUsername(username);
    }

    private String normalizeUsername(String rawUsername) {
        return rawUsername == null ? "" : rawUsername.trim();
    }

    private String normalizePassword(String rawPassword) {
        return rawPassword == null ? "" : rawPassword.trim();
    }

    private void validateRegistrationInput(String username, String password) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Username phai dai 3-30 ky tu va chi gom chu, so, _ hoac -.");
        }

        if (password.length() < 6 || password.length() > 72) {
            throw new IllegalArgumentException("Password phai dai tu 6 den 72 ky tu.");
        }
    }
}
