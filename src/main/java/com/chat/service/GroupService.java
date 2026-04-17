package com.chat.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.chat.dto.GroupResponse;
import com.chat.entity.GroupRoom;
import com.chat.repository.GroupRoomRepository;

@Service
public class GroupService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final GroupRoomRepository groupRoomRepository;
    private final UserService userService;

    public GroupService(GroupRoomRepository groupRoomRepository, UserService userService) {
        this.groupRoomRepository = groupRoomRepository;
        this.userService = userService;
    }

    public GroupResponse createGroup(String ownerUsername, String rawName, boolean autoApproveJoin) {
        String name = normalizeName(rawName);
        if (name.length() < 3 || name.length() > 80) {
            throw new IllegalArgumentException("Ten nhom phai dai tu 3 den 80 ky tu.");
        }

        GroupRoom groupRoom = new GroupRoom();
        groupRoom.setGroupCode(generateUniqueCode());
        groupRoom.setName(name);
        groupRoom.setOwnerUsername(normalizeUsername(ownerUsername));
        groupRoom.setAutoApproveJoin(autoApproveJoin);
        groupRoom.addMember(normalizeUsername(ownerUsername));
        groupRoom.touchForCreate();

        return toResponse(groupRoomRepository.save(groupRoom));
    }

    public GroupResponse joinGroup(String username, String groupCode) {
        GroupRoom groupRoom = requireGroup(groupCode);
        String cleanUsername = normalizeUsername(username);

        if (groupRoom.getMemberUsernames().contains(cleanUsername)) {
            return toResponse(groupRoom);
        }

        if (groupRoom.isAutoApproveJoin()) {
            groupRoom.addMember(cleanUsername);
        } else {
            groupRoom.addPendingMember(cleanUsername);
        }

        groupRoom.touchForUpdate();
        return toResponse(groupRoomRepository.save(groupRoom));
    }

    public GroupResponse inviteMember(String actorUsername, String groupCode, String invitedUsername) {
        GroupRoom groupRoom = requireGroup(groupCode);
        String actor = normalizeUsername(actorUsername);
        String invited = normalizeUsername(invitedUsername);

        assertCanManageGroup(actor, groupRoom);

        if (groupRoom.getMemberUsernames().contains(invited)) {
            return toResponse(groupRoom);
        }

        if (!userService.existsByUsername(invited)) {
            throw new IllegalArgumentException("Nguoi dung duoc moi khong ton tai.");
        }

        if (groupRoom.isAutoApproveJoin()) {
            groupRoom.addMember(invited);
        } else {
            groupRoom.addPendingMember(invited);
        }

        groupRoom.touchForUpdate();
        return toResponse(groupRoomRepository.save(groupRoom));
    }

    public GroupResponse approveMember(String actorUsername, String groupCode, String username) {
        GroupRoom groupRoom = requireGroup(groupCode);
        assertCanManageGroup(normalizeUsername(actorUsername), groupRoom);

        String cleanUsername = normalizeUsername(username);
        if (!groupRoom.getPendingUsernames().contains(cleanUsername)) {
            throw new IllegalArgumentException("Nguoi dung khong nam trong danh sach cho duyet.");
        }

        groupRoom.addMember(cleanUsername);
        groupRoom.touchForUpdate();
        return toResponse(groupRoomRepository.save(groupRoom));
    }

    public GroupResponse toggleAutoApprove(String actorUsername, String groupCode, boolean autoApproveJoin) {
        GroupRoom groupRoom = requireGroup(groupCode);
        assertCanManageGroup(normalizeUsername(actorUsername), groupRoom);
        groupRoom.setAutoApproveJoin(autoApproveJoin);
        groupRoom.touchForUpdate();
        return toResponse(groupRoomRepository.save(groupRoom));
    }

    public List<GroupResponse> listGroupsForUser(String username) {
        return groupRoomRepository.findByMemberUsernamesContainingOrderByCreatedAtDesc(normalizeUsername(username))
                .stream()
                .map(this::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public GroupResponse getGroup(String groupCode) {
        return toResponse(requireGroup(groupCode));
    }

    public void requireMember(String username, String groupCode) {
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