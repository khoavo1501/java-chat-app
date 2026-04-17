package com.chat.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.chat.dto.GroupResponse;
import com.chat.entity.GroupRoom;
import com.chat.entity.UserAccount;
import com.chat.repository.GroupRoomRepository;

@Service
public class GroupService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final GroupRoomRepository groupRoomRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public GroupService(GroupRoomRepository groupRoomRepository,
            UserService userService,
            NotificationService notificationService) {
        this.groupRoomRepository = groupRoomRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    public GroupResponse createGroup(String ownerUsername, String rawName, boolean autoApproveJoin) {
        UserAccount owner = userService.requireActiveUser(ownerUsername);
        String name = normalizeName(rawName);
        if (name.length() < 3 || name.length() > 80) {
            throw new IllegalArgumentException("Ten nhom phai dai tu 3 den 80 ky tu.");
        }

        GroupRoom groupRoom = new GroupRoom();
        groupRoom.setGroupCode(generateUniqueCode());
        groupRoom.setName(name);
        groupRoom.setOwnerUsername(owner.getUsername());
        groupRoom.setAutoApproveJoin(autoApproveJoin);
        groupRoom.addMember(owner.getUsername());
        groupRoom.touchForCreate();

        return toResponse(groupRoomRepository.save(groupRoom));
    }

    public GroupResponse joinGroup(String username, String groupCode) {
        String cleanUsername = userService.requireActiveUser(username).getUsername();
        GroupRoom groupRoom = requireGroup(groupCode);

        if (groupRoom.getMemberUsernames().contains(cleanUsername)) {
            return toResponse(groupRoom);
        }

        if (groupRoom.isAutoApproveJoin()) {
            groupRoom.addMember(cleanUsername);
        } else {
            groupRoom.addPendingMember(cleanUsername);
        }

        groupRoom.touchForUpdate();
        GroupRoom saved = groupRoomRepository.save(groupRoom);

        if (!saved.isAutoApproveJoin() && !saved.getOwnerUsername().equals(cleanUsername)) {
            notificationService.sendToUser(
                    saved.getOwnerUsername(),
                    "GROUP_JOIN_REQUEST",
                    "Yeu cau vao nhom moi",
                    cleanUsername + " dang yeu cau tham gia nhom " + saved.getName() + ".",
                    cleanUsername,
                    saved.getGroupCode());
        }

        return toResponse(saved);
    }

    public GroupResponse inviteMember(String actorUsername, String groupCode, String invitedUsername) {
        GroupRoom groupRoom = requireGroup(groupCode);
        String actor = userService.requireActiveUser(actorUsername).getUsername();
        String invited = userService.requireActiveUser(invitedUsername).getUsername();

        assertCanManageGroup(actor, groupRoom);

        if (groupRoom.getMemberUsernames().contains(invited)) {
            return toResponse(groupRoom);
        }

        if (groupRoom.isAutoApproveJoin()) {
            groupRoom.addMember(invited);
        } else {
            groupRoom.addPendingMember(invited);
        }

        groupRoom.touchForUpdate();
        GroupRoom saved = groupRoomRepository.save(groupRoom);

        notificationService.sendToUser(
                invited,
                "GROUP_INVITE",
                "Loi moi vao nhom",
                groupRoom.isAutoApproveJoin()
                        ? "Ban da duoc them vao nhom " + saved.getName() + " boi " + actor + "."
                        : actor + " da moi ban vao nhom " + saved.getName() + ".",
                actor,
                saved.getGroupCode());

        return toResponse(saved);
    }

    public GroupResponse approveMember(String actorUsername, String groupCode, String username) {
        GroupRoom groupRoom = requireGroup(groupCode);
        String actor = userService.requireActiveUser(actorUsername).getUsername();
        assertCanManageGroup(actor, groupRoom);

        String cleanUsername = userService.requireActiveUser(username).getUsername();
        if (!groupRoom.getPendingUsernames().contains(cleanUsername)) {
            throw new IllegalArgumentException("Nguoi dung khong nam trong danh sach cho duyet.");
        }

        groupRoom.addMember(cleanUsername);
        groupRoom.touchForUpdate();
        GroupRoom saved = groupRoomRepository.save(groupRoom);

        if (!cleanUsername.equals(actor)) {
            notificationService.sendToUser(
                    cleanUsername,
                    "GROUP_JOIN_APPROVED",
                    "Yeu cau vao nhom duoc duyet",
                    actor + " da duyet ban vao nhom " + saved.getName() + ".",
                    actor,
                    saved.getGroupCode());
        }

        return toResponse(saved);
    }

    public GroupResponse toggleAutoApprove(String actorUsername, String groupCode, boolean autoApproveJoin) {
        GroupRoom groupRoom = requireGroup(groupCode);
        String actor = userService.requireActiveUser(actorUsername).getUsername();
        assertCanManageGroup(actor, groupRoom);
        groupRoom.setAutoApproveJoin(autoApproveJoin);
        groupRoom.touchForUpdate();
        return toResponse(groupRoomRepository.save(groupRoom));
    }

    public List<GroupResponse> listGroupsForUser(String username) {
        UserAccount requester = userService.requireActiveUser(username);

        return groupRoomRepository.findByMemberUsernamesContainingOrderByCreatedAtDesc(requester.getUsername())
                .stream()
                .map(group -> requester.hasRole("ADMIN")
                        ? toResponse(group)
                        : toResponseForRegularUser(group, requester.getUsername()))
                .collect(Collectors.toList());
    }

    public GroupResponse getGroup(String groupCode) {
        return toResponse(requireGroup(groupCode));
    }

    public GroupResponse getGroupForUser(String username, String groupCode) {
        UserAccount requester = userService.requireActiveUser(username);
        GroupRoom group = requireGroup(groupCode);

        if (requester.hasRole("ADMIN")) {
            return toResponse(group);
        }

        return toResponseForRegularUser(group, requester.getUsername());
    }

    public List<String> getMemberUsernames(String groupCode) {
        return new ArrayList<>(requireGroup(groupCode).getMemberUsernames());
    }

    public List<GroupResponse> listAllGroups() {
        return groupRoomRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(GroupRoom::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse toggleAutoApproveAsAdmin(String groupCode, boolean autoApproveJoin) {
        GroupRoom groupRoom = requireGroup(groupCode);
        groupRoom.setAutoApproveJoin(autoApproveJoin);
        groupRoom.touchForUpdate();
        return toResponse(groupRoomRepository.save(groupRoom));
    }

    public void deleteGroupAsAdmin(String groupCode) {
        GroupRoom groupRoom = requireGroup(groupCode);
        groupRoomRepository.delete(groupRoom);
    }

    public long countAllGroups() {
        return groupRoomRepository.count();
    }

    public long countPendingJoinRequests() {
        return groupRoomRepository.findAll()
                .stream()
                .mapToLong(group -> group.getPendingUsernames().size())
                .sum();
    }

    public void requireMember(String username, String groupCode) {
        userService.requireActiveUser(username);
        GroupRoom groupRoom = requireGroup(groupCode);
        if (!groupRoom.getMemberUsernames().contains(normalizeUsername(username))) {
            throw new IllegalArgumentException("Ban khong phai thanh vien cua nhom nay.");
        }
    }

    private GroupRoom requireGroup(String groupCode) {
        String cleanCode = normalizeCode(groupCode);
        return groupRoomRepository.findByGroupCode(cleanCode)
                .orElseThrow(() -> new IllegalArgumentException("Nhom khong ton tai: " + cleanCode));
    }

    private void assertCanManageGroup(String username, GroupRoom groupRoom) {
        if (groupRoom.getOwnerUsername().equals(username) || userService.isAdmin(username)) {
            return;
        }

        throw new IllegalArgumentException("Chi chu nhom hoac admin moi co quyen thuc hien thao tac nay.");
    }

    private GroupResponse toResponse(GroupRoom groupRoom) {
        GroupResponse response = new GroupResponse();
        response.setGroupCode(groupRoom.getGroupCode());
        response.setName(groupRoom.getName());
        response.setOwnerUsername(groupRoom.getOwnerUsername());
        response.setMemberUsernames(new ArrayList<>(groupRoom.getMemberUsernames()));
        response.setPendingUsernames(new ArrayList<>(groupRoom.getPendingUsernames()));
        response.setAutoApproveJoin(groupRoom.isAutoApproveJoin());
        response.setCreatedAt(groupRoom.getCreatedAt());
        return response;
    }

    private GroupResponse toResponseForRegularUser(GroupRoom groupRoom, String requesterUsername) {
        GroupResponse response = toResponse(groupRoom);

        if (!userService.isVisibleToRegularUsers(response.getOwnerUsername())
                && !response.getOwnerUsername().equals(requesterUsername)) {
            response.setOwnerUsername("tai-khoan-an");
        }

        response.setMemberUsernames(response.getMemberUsernames()
                .stream()
                .filter(username -> username.equals(requesterUsername) || userService.isVisibleToRegularUsers(username))
                .collect(Collectors.toList()));

        response.setPendingUsernames(response.getPendingUsernames()
                .stream()
                .filter(userService::isVisibleToRegularUsers)
                .collect(Collectors.toList()));

        return response;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = generateCode(8);
            if (!groupRoomRepository.findByGroupCode(code).isPresent()) {
                return code;
            }
        }

        throw new IllegalStateException("Khong the tao ma nhom moi.");
    }

    private String generateCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        }
        return builder.toString();
    }

    private String normalizeUsername(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}