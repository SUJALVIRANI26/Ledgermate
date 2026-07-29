package com.college.ledgermate.service;

import com.college.ledgermate.model.Group;
import com.college.ledgermate.model.User;
import com.college.ledgermate.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserService userService;

    public GroupService(GroupRepository groupRepository, UserService userService) {
        this.groupRepository = groupRepository;
        this.userService = userService;
    }

    @Transactional
    public Group createGroup(String name, List<String> memberEmails) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        Set<User> members = new HashSet<>();
        for (String email : memberEmails) {
            members.add(userService.getByEmail(email));
        }
        Group group = Group.builder()
                .name(name)
                .members(members)
                .build();
        if (group == null) {
            throw new IllegalArgumentException("Failed to create group");
        }
        return groupRepository.save(group);
    }

    @Transactional
    public Group addMember(Long groupId, String email) {
        if (groupId == null) {
            throw new IllegalArgumentException("Group id cannot be null");
        }
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        User user = userService.getByEmail(email);
        group.getMembers().add(user);
        return groupRepository.save(group);
    }

    public Group getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Group id cannot be null");
        }
        return groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
    }
}

