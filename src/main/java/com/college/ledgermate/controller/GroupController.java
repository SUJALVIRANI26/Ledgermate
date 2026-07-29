package com.college.ledgermate.controller;

import com.college.ledgermate.dto.GroupDtos;
import com.college.ledgermate.model.Group;
import com.college.ledgermate.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<Group> createGroup(
            @Valid @RequestBody GroupDtos.CreateGroupRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName(); // logged-in user email

        List<String> memberEmails = new ArrayList<>(request.getMemberEmails());

        if (!memberEmails.contains(userEmail)) {
            memberEmails.add(userEmail);
        }

        Group group = groupService.createGroup(request.getName(), memberEmails);
        return ResponseEntity.ok(group);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{groupId}/members")
    public ResponseEntity<Group> addMember(@PathVariable Long groupId,
                                           @Valid @RequestBody GroupDtos.AddMemberRequest request) {
        Group group = groupService.addMember(groupId, request.getEmail());
        return ResponseEntity.ok(group);

    }
}

