package com.chat.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chat.dto.UserPresenceResponse;
import com.chat.dto.UserProfileResponse;
import com.chat.entity.UserAccount;
import com.chat.repository.UserAccountRepository;

@Service
public class UserService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,30}$");
    private static final Pattern THEME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");
    private static final Set<String> ALLOWED_THEMES = new LinkedHashSet<>(Arrays.asList(
            "aurora",
            "midnight",
            "sunrise",
            "forest"));

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public UserService(UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

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
        user.setActive(true);
        user.setOnline(false);
        user.setTheme(defaultTheme());
        user.addRole("USER");
        user.touchForCreate();
        return userAccountRepository.save(user);
    }

    public List<UserPresenceResponse> getAllPresence() {
        return userAccountRepository.findAllByOrderByUsernameAsc()
                .stream()
                .filter(this::isVisibleToRegularUsers)
                .map(user -> new UserPresenceResponse(
                        user.getUsername(),
                        user.isOnline(),
                        safeTheme(user.getTheme())))
                .collect(Collectors.toList());
    }

    public List<UserProfileResponse> listAllProfiles() {
        return userAccountRepository.findAllByOrderByUsernameAsc()
                .stream()
                .map(this::toProfileResponse)
                .collect(Collectors.toList());
    }

    public UserProfileResponse getProfile(String username) {
        UserAccount currentUser = requireUser(username);
        UserProfileResponse response = toProfileResponse(currentUser);

        if (!currentUser.hasRole("ADMIN")) {
            response.setFriends(filterVisibleUsernames(response.getFriends()));
            response.setIncomingFriendRequests(filterVisibleUsernames(response.getIncomingFriendRequests()));
        }

        return response;
    }

    public void updateOnline(String username, boolean online) {
        userAccountRepository.findByUsername(normalizeUsername(username)).ifPresent(user -> {
            boolean nextOnline = user.isActive() ? online : false;
            if (user.isOnline() != nextOnline) {
                user.setOnline(nextOnline);
                user.touchForUpdate();
                userAccountRepository.save(user);
            }
        });
    }

    public void updateTheme(String username, String theme) {
        UserAccount user = requireUser(username);
        user.setTheme(validateTheme(theme));
        user.touchForUpdate();
        userAccountRepository.save(user);
    }

    public void sendFriendRequest(String requesterUsername, String targetUsername) {
        String requester = normalizeUsername(requesterUsername);
        String target = normalizeUsername(targetUsername);

        if (requester.isEmpty() || target.isEmpty()) {
            throw new IllegalArgumentException("Username khong hop le.");
        }

        if (requester.equalsIgnoreCase(target)) {
            throw new IllegalArgumentException("Khong the them chinh minh.");
        }

        UserAccount requesterUser = requireActiveUser(requester);
        UserAccount targetUser = requireActiveUser(target);

        if (!isVisibleToRegularUsers(targetUser) && !requesterUser.hasRole("ADMIN")) {
            throw new IllegalArgumentException("Nguoi dung khong ton tai.");
        }

        if (requesterUser.getFriends().contains(targetUser.getUsername())) {
            throw new IllegalArgumentException("Da la ban be.");
        }

        if (targetUser.getIncomingFriendRequests().contains(requesterUser.getUsername())) {
            throw new IllegalArgumentException("Da gui yeu cau truoc do.");
        }

        targetUser.addIncomingFriendRequest(requesterUser.getUsername());
        targetUser.touchForUpdate();
        userAccountRepository.save(targetUser);

        notificationService.sendToUser(
                targetUser.getUsername(),
                "FRIEND_REQUEST",
                "Yeu cau ket ban moi",
                requesterUser.getUsername() + " da gui yeu cau ket ban cho ban.",
                requesterUser.getUsername(),
                null);
    }

    public void acceptFriendRequest(String username, String requesterUsername) {
        UserAccount currentUser = requireActiveUser(username);
        UserAccount requesterUser = requireActiveUser(requesterUsername);

        if (!currentUser.getIncomingFriendRequests().contains(requesterUser.getUsername())) {
            throw new IllegalArgumentException("Khong co yeu cau ket ban phu hop.");
        }

        currentUser.removeIncomingFriendRequest(requesterUser.getUsername());
        currentUser.addFriend(requesterUser.getUsername());
        requesterUser.addFriend(currentUser.getUsername());
        currentUser.touchForUpdate();
        requesterUser.touchForUpdate();

        userAccountRepository.save(currentUser);
        userAccountRepository.save(requesterUser);

        notificationService.sendToUser(
                requesterUser.getUsername(),
                "FRIEND_REQUEST_APPROVED",
                "Yeu cau ket ban duoc duyet",
                currentUser.getUsername() + " da chap nhan loi moi ket ban cua ban.",
                currentUser.getUsername(),
                null);
    }

    public void rejectFriendRequest(String username, String requesterUsername) {
        UserAccount currentUser = requireUser(username);
        if (currentUser.getIncomingFriendRequests().remove(normalizeUsername(requesterUsername))) {
            currentUser.touchForUpdate();
            userAccountRepository.save(currentUser);
        }
    }

    public void removeFriend(String username, String friendUsername) {
        UserAccount currentUser = requireUser(username);
        UserAccount friendUser = requireUser(friendUsername);

        currentUser.removeFriend(friendUser.getUsername());
        friendUser.removeFriend(currentUser.getUsername());
        currentUser.touchForUpdate();
        friendUser.touchForUpdate();

        userAccountRepository.save(currentUser);
        userAccountRepository.save(friendUser);
    }

    public void updateRole(String username, String role, boolean enabled) {
        String normalizedRole = normalizeRole(role);
        if (!"ADMIN".equals(normalizedRole) && !"USER".equals(normalizedRole)) {
            throw new IllegalArgumentException("Role khong hop le.");
        }

        if ("USER".equals(normalizedRole) && !enabled) {
            throw new IllegalArgumentException("Khong the huy role USER.");
        }

        UserAccount user = requireUser(username);
        if (enabled) {
            user.addRole(normalizedRole);
        } else {
            user.removeRole(normalizedRole);
        }

        user.addRole("USER");
        user.touchForUpdate();
        userAccountRepository.save(user);
    }

    public UserProfileResponse updateAccountStatus(String actorUsername, String targetUsername, boolean active) {
        String actor = normalizeUsername(actorUsername);
        UserAccount target = requireUser(targetUsername);

        if (!active && target.getUsername().equals(actor)) {
            throw new IllegalArgumentException("Khong the tu vo hieu hoa tai khoan cua chinh minh.");
        }

        if (!active && target.hasRole("ADMIN")) {
            throw new IllegalArgumentException("Khong the vo hieu hoa tai khoan ADMIN.");
        }

        target.setActive(active);
        if (!active) {
            target.setOnline(false);
        }

        target.touchForUpdate();
        return toProfileResponse(userAccountRepository.save(target));
    }

    public boolean existsByUsername(String username) {
        return userAccountRepository.existsByUsername(normalizeUsername(username));
    }

    public UserAccount requireActiveUser(String username) {
        UserAccount user = requireUser(username);
        if (!user.isActive()) {
            throw new IllegalArgumentException("Tai khoan da bi vo hieu hoa.");
        }

        return user;
    }

    public boolean isVisibleToRegularUsers(String username) {
        String cleanUsername = normalizeUsername(username);
        if (cleanUsername.isEmpty()) {
            return false;
        }

        return userAccountRepository.findByUsername(cleanUsername)
                .map(this::isVisibleToRegularUsers)
                .orElse(false);
    }

    public boolean isAdmin(String username) {
        return requireUser(username).hasRole("ADMIN");
    }

    public long countAllUsers() {
        return userAccountRepository.count();
    }

    public long countOnlineUsers() {
        return userAccountRepository.findAllByOrderByUsernameAsc()
                .stream()
                .filter(UserAccount::isOnline)
                .count();
    }

    public long countAdminUsers() {
        return userAccountRepository.findAllByOrderByUsernameAsc()
                .stream()
                .filter(user -> user.hasRole("ADMIN"))
                .count();
    }

    public long countPendingFriendRequests() {
        return userAccountRepository.findAllByOrderByUsernameAsc()
                .stream()
                .mapToLong(user -> user.getIncomingFriendRequests().size())
                .sum();
    }

    private UserProfileResponse toProfileResponse(UserAccount user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUsername(user.getUsername());
        response.setActive(user.isActive());
        response.setOnline(user.isActive() && user.isOnline());
        response.setTheme(safeTheme(user.getTheme()));
        response.setRoles(new ArrayList<>(user.getRoles()));
        response.setFriends(new ArrayList<>(user.getFriends()));
        response.setIncomingFriendRequests(new ArrayList<>(user.getIncomingFriendRequests()));
        return response;
    }

    private boolean isVisibleToRegularUsers(UserAccount user) {
        return user != null && user.isActive() && !user.hasRole("ADMIN");
    }

    private List<String> filterVisibleUsernames(List<String> usernames) {
        return usernames
                .stream()
                .map(this::normalizeUsername)
                .filter(username -> !username.isEmpty())
                .filter(this::isVisibleToRegularUsers)
                .collect(Collectors.toList());
    }

    private UserAccount requireUser(String username) {
        String cleanUsername = normalizeUsername(username);
        return userAccountRepository.findByUsername(cleanUsername)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung: " + cleanUsername));
    }

    private String normalizeUsername(String rawUsername) {
        return rawUsername == null ? "" : rawUsername.trim();
    }

    private String normalizePassword(String rawPassword) {
        return rawPassword == null ? "" : rawPassword.trim();
    }

    private String validateTheme(String theme) {
        String normalized = theme == null ? "" : theme.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return defaultTheme();
        }

        if (!THEME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Theme khong hop le.");
        }

        if (!ALLOWED_THEMES.contains(normalized)) {
            throw new IllegalArgumentException("Theme khong duoc ho tro.");
        }

        return normalized;
    }

    private String safeTheme(String theme) {
        String normalized = theme == null ? "" : theme.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_THEMES.contains(normalized) ? normalized : defaultTheme();
    }

    private String defaultTheme() {
        return "aurora";
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
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