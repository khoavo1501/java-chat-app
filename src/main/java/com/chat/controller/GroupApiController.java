package com.chat.controller;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chat.dto.GroupAutoApproveRequest;
import com.chat.dto.GroupCreateRequest;
import com.chat.dto.GroupInviteRequest;
import com.chat.dto.GroupJoinRequest;
import com.chat.dto.GroupResponse;
import com.chat.service.GroupService;

@RestController
@RequestMapping("/api/groups")
public class GroupApiController {

    private final GroupService groupService;

    public GroupApiController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<GroupResponse> listGroups(Principal principal) {
        if (principal == null) {
            return Collections.emptyList();
        }

        return groupService.listGroupsForUser(principal.getName());
    }

    @GetMapping("/{groupCode:[A-Z0-9]{8}}")
    public GroupResponse getGroup(@PathVariable("groupCode") String groupCode, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Nguoi dung chua dang nhap.");
        }

        groupService.requireMember(principal.getName(), groupCode);
        return groupService.getGroup(groupCode);
    }

    @PostMapping
    public GroupResponse createGroup(@Valid @RequestBody GroupCreateRequest request, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Nguoi dung chua dang nhap.");
        }

        return groupService.createGroup(principal.getName(), request.getName(), request.isAutoApproveJoin());
    }

    @PostMapping("/join")
    public GroupResponse joinGroup(@Valid @RequestBody GroupJoinRequest request, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Nguoi dung chua dang nhap.");
        }

        return groupService.joinGroup(principal.getName(), request.getGroupCode());
    }

    @PostMapping("/{groupCode}/invite")
    public GroupResponse inviteMember(@PathVariable("groupCode") String groupCode,
            @Valid @RequestBody GroupInviteRequest request,
            Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Nguoi dung chua dang nhap.");
        }

        return groupService.inviteMember(principal.getName(), groupCode, request.getUsername());
    }

    @PostMapping("/{groupCode}/approve/{username}")
    public GroupResponse approveMember(@PathVariable("groupCode") String groupCode,
            @PathVariable("username") String username,
            Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Nguoi dung chua dang nhap.");
        }

        return groupService.approveMember(principal.getName(), groupCode, username);
    }

    @PatchMapping("/{groupCode}/auto-approve")
    public GroupResponse toggleAutoApprove(@PathVariable("groupCode") String groupCode,
            @Valid @RequestBody GroupAutoApproveRequest request,
            Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Nguoi dung chua dang nhap.");
        }

        return groupService.toggleAutoApprove(principal.getName(), groupCode, request.isAutoApproveJoin());
    }
}