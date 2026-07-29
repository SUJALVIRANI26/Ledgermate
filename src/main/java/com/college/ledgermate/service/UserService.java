package com.college.ledgermate.service;

import com.college.ledgermate.model.Expense;
import com.college.ledgermate.model.ExpenseShare;
import com.college.ledgermate.model.Group;
import com.college.ledgermate.model.Settlement;
import com.college.ledgermate.model.User;
import com.college.ledgermate.repository.ExpenseRepository;
import com.college.ledgermate.repository.ExpenseShareRepository;
import com.college.ledgermate.repository.GroupRepository;
import com.college.ledgermate.repository.SettlementRepository;
import com.college.ledgermate.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final SettlementRepository settlementRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            GroupRepository groupRepository,
            ExpenseRepository expenseRepository,
            ExpenseShareRepository expenseShareRepository,
            SettlementRepository settlementRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.expenseShareRepository = expenseShareRepository;
        this.settlementRepository = settlementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(String name, String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .roles(Collections.singleton("USER"))
                .build();
        if (user == null) {
            throw new IllegalArgumentException("Failed to create user");
        }
        return userRepository.save(user);
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    public User getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    @Transactional
    public User updateUser( String name, String email, String rawPassword) {
        if (!userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email does not registered");
        }
        User user = getByEmail(email);

        user.setName(name);
//        user.setEmail(email);

        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        // Remove user from all groups first to keep many-to-many owner state clean
        List<Group> groups = groupRepository.findByMembersContaining(user);
        for (Group group : groups) {
            group.getMembers().remove(user);
            groupRepository.save(group);
        }

        // Remove financial traces to avoid FK constraint issues
        List<Settlement> sentSettlements = settlementRepository.findSettlementsSentByUser(user);
        if (sentSettlements != null) {
            settlementRepository.deleteAll(sentSettlements);
        }
        List<Settlement> receivedSettlements = settlementRepository.findSettlementsReceivedByUser(user);
        if (receivedSettlements != null) {
            settlementRepository.deleteAll(receivedSettlements);
        }
        List<ExpenseShare> expenseShares = expenseShareRepository.findByUser(user);
        if (expenseShares != null) {
            expenseShareRepository.deleteAll(expenseShares);
        }
        List<Expense> expenses = expenseRepository.findByPaidBy(user);
        if (expenses != null) {
            expenseRepository.deleteAll(expenses);
        }

        userRepository.delete(user);
    }
}
