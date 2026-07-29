package com.college.ledgermate.service;

import com.college.ledgermate.dto.BalanceDto;
import com.college.ledgermate.model.*;
import com.college.ledgermate.repository.ExpenseRepository;
import com.college.ledgermate.repository.ExpenseShareRepository;
import com.college.ledgermate.repository.GroupRepository;
import com.college.ledgermate.repository.SettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class ExpenseService {

    public enum SplitType {
        EQUAL,
        CUSTOM
    }

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;
    private final UserService userService;

    public ExpenseService(ExpenseRepository expenseRepository,
            SettlementRepository settlementRepository,
            ExpenseShareRepository expenseShareRepository,
            GroupRepository groupRepository,
            GroupService groupService,
            UserService userService) {
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.expenseShareRepository = expenseShareRepository;
        this.groupRepository = groupRepository;
        this.groupService = groupService;
        this.userService = userService;
    }

    @Transactional
    public Expense addExpense(Long groupId,
            String description,
            BigDecimal amount,
            String paidByEmail,
            SplitType splitType,
            List<String> participantEmails,
            Map<String, BigDecimal> customSplits) {

        Group group = groupService.getById(groupId);
        User paidBy = userService.getByEmail(paidByEmail);

        if (!group.getMembers().contains(paidBy)) {
            throw new IllegalArgumentException("Payer must be a member of the group");
        }

        List<User> participants = participantEmails.stream()
                .map(userService::getByEmail)
                .peek(u -> {
                    if (!group.getMembers().contains(u)) {
                        throw new IllegalArgumentException("All participants must be group members");
                    }
                })
                .toList();

        // Create expense without shares first
        Expense expense = Expense.builder()
                .description(description)
                .amount(amount)
                .group(group)
                .paidBy(paidBy)
                .createdAt(LocalDateTime.now())
                .shares(new ArrayList<>()) // Initialize empty list
                .build();

        // Save expense first to get ID
        if (expense == null) {
            throw new IllegalArgumentException("Failed to create expense");
        }
        Expense savedExpense = expenseRepository.save(expense);

        // Create shares with the saved expense
        List<ExpenseShare> shares = new ArrayList<>();

        if (splitType == SplitType.EQUAL) {
            BigDecimal perHead = amount
                    .divide(BigDecimal.valueOf(participants.size()), 2, RoundingMode.HALF_UP);
            for (User u : participants) {
                ExpenseShare share = ExpenseShare.builder()
                        .expense(savedExpense) // Set the saved expense
                        .user(u)
                        .amount(perHead)
                        .build();
                shares.add(share);
            }
        } else {
            BigDecimal total = customSplits.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(amount) != 0) {
                throw new IllegalArgumentException("Custom split amounts must sum to total amount");
            }
            for (Map.Entry<String, BigDecimal> entry : customSplits.entrySet()) {
                User u = userService.getByEmail(entry.getKey());
                if (!participants.contains(u)) {
                    throw new IllegalArgumentException("Custom split user must be in participants list");
                }
                ExpenseShare share = ExpenseShare.builder()
                        .expense(savedExpense) // Set the saved expense
                        .user(u)
                        .amount(entry.getValue())
                        .build();
                shares.add(share);
            }
        }

        // Save all shares
        List<ExpenseShare> savedShares = expenseShareRepository.saveAll(shares);

        // Set shares back to expense
        savedExpense.setShares(savedShares);

        return savedExpense;
    }

    @Transactional
    public Settlement recordSettlement(Long groupId, String fromEmail, String toEmail, BigDecimal amount) {
        Group group = groupService.getById(groupId);
        User from = userService.getByEmail(fromEmail);
        User to = userService.getByEmail(toEmail);

        if (!group.getMembers().contains(from) || !group.getMembers().contains(to)) {
            throw new IllegalArgumentException("Both users must be in the group");
        }

        if (from.equals(to)) {
            throw new IllegalArgumentException("Cannot settle with yourself");
        }

        // Calculate EXACTLY how much fromUser owes toUser
        BigDecimal amountOwedToTo = getPairwiseAmountOwed(groupId, fromEmail, toEmail);

        if (amountOwedToTo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    String.format("%s does not owe money to %s", from.getName(), to.getName()));
        }

        if (amount.compareTo(amountOwedToTo) > 0) {
            throw new IllegalArgumentException(
                    String.format("Cannot settle %.2f. You only owe %.2f to %s",
                            amount, amountOwedToTo, to.getName()));
        }

        Settlement settlement = Settlement.builder()
                .group(group)
                .fromUser(from)
                .toUser(to)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();
        if(settlement == null) {
            throw new IllegalArgumentException("Failed to create settlement");
        }

        return settlementRepository.save(settlement);
    }

    // NEW HELPER: Calculates the exact debt between two specific users
    private BigDecimal getPairwiseAmountOwed(Long groupId, String fromEmail, String toEmail) {
        Group group = groupService.getById(groupId);
        BigDecimal amountOwed = BigDecimal.ZERO;

        // 1. Add debts from expenses
        List<Expense> expenses = expenseRepository.findByGroup(group);
        for (Expense expense : expenses) {
            String paidBy = expense.getPaidBy().getEmail();
            List<ExpenseShare> shares = expenseShareRepository.findByExpense(expense);

            for (ExpenseShare share : shares) {
                String shareUser = share.getUser().getEmail();
                if (shareUser.equals(fromEmail) && paidBy.equals(toEmail)) {
                    amountOwed = amountOwed.add(share.getAmount()); // from owes to
                } else if (shareUser.equals(toEmail) && paidBy.equals(fromEmail)) {
                    amountOwed = amountOwed.subtract(share.getAmount()); // to owes from (reduces debt)
                }
            }
        }

        // 2. Subtract debts that have already been settled
        List<Settlement> settlements = settlementRepository.findByGroup(group);
        for (Settlement settlement : settlements) {
            String from = settlement.getFromUser().getEmail();
            String to = settlement.getToUser().getEmail();
            BigDecimal amount = settlement.getAmount();

            if (from.equals(fromEmail) && to.equals(toEmail)) {
                amountOwed = amountOwed.subtract(amount); // payment reduces debt
            } else if (from.equals(toEmail) && to.equals(fromEmail)) {
                amountOwed = amountOwed.add(amount); // reverse payment increases debt
            }
        }

        return amountOwed;
    }

    // private BigDecimal getSpecificAmountOwed(Map<String, BigDecimal> balances, String fromEmail, String toEmail) {
    //     BigDecimal fromBalance = balances.getOrDefault(fromEmail, BigDecimal.ZERO);

    //     if (fromBalance.compareTo(BigDecimal.ZERO) >= 0) {
    //         return BigDecimal.ZERO;
    //     }

    //     BigDecimal toBalance = balances.getOrDefault(toEmail, BigDecimal.ZERO);

    //     if (toBalance.compareTo(BigDecimal.ZERO) <= 0) {
    //         return BigDecimal.ZERO;
    //     }

    //     // Get all positive balances
    //     List<BigDecimal> positiveBalances = balances.values().stream()
    //             .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
    //             .toList();

    //     BigDecimal totalPositive = positiveBalances.stream()
    //             .reduce(BigDecimal.ZERO, BigDecimal::add);

    //     // Calculate proportion of from's debt that goes to to
    //     BigDecimal proportion = toBalance.divide(totalPositive, 10, RoundingMode.HALF_UP);
    //     BigDecimal amountOwed = fromBalance.abs().multiply(proportion).setScale(2, RoundingMode.HALF_UP);

    //     return amountOwed;
    // }

    /**
     * Calculate all balances in a group
     * This method properly calculates balances from expense_shares table
     */
    public Map<String, BigDecimal> calculateGroupBalances(Long groupId) {
        Group group = groupService.getById(groupId);

        // Initialize balances
        Map<String, BigDecimal> balances = new HashMap<>();
        for (User member : group.getMembers()) {
            balances.put(member.getEmail(), BigDecimal.ZERO);
        }

        // Get all expenses
        List<Expense> expenses = expenseRepository.findByGroup(group);

        // Process each expense
        for (Expense expense : expenses) {
            String paidBy = expense.getPaidBy().getEmail();
            BigDecimal totalAmount = expense.getAmount();

            // Add full amount to payer
            balances.put(paidBy, balances.get(paidBy).add(totalAmount));

            // Subtract each participant's share
            for (ExpenseShare share : expense.getShares()) {
                String userEmail = share.getUser().getEmail();
                BigDecimal shareAmount = share.getAmount();
                balances.put(userEmail, balances.get(userEmail).subtract(shareAmount));
            }
        }

        // Process settlements
        List<Settlement> settlements = settlementRepository.findByGroup(group);
        for (Settlement settlement : settlements) {
            String fromEmail = settlement.getFromUser().getEmail();
            String toEmail = settlement.getToUser().getEmail();
            BigDecimal amount = settlement.getAmount();

            balances.put(fromEmail, balances.get(fromEmail).subtract(amount));
            balances.put(toEmail, balances.get(toEmail).add(amount));
        }

        return balances;
    }

    /**
     * Get simplified group balance for a specific user
     */
    public BalanceDto.GroupBalanceResponse getGroupBalanceForUser(Long groupId, String userEmail) {
        Group group = groupService.getById(groupId);
        User currentUser = userService.getByEmail(userEmail);

        if (!group.getMembers().contains(currentUser)) {
            throw new IllegalArgumentException("You are not a member of this group");
        }

        // 1. Map to track EXACTLY who owes whom in the group
        Map<String, Map<String, BigDecimal>> owesMap = new HashMap<>();

        List<Expense> expenses = expenseRepository.findByGroup(group);
        for (Expense expense : expenses) {
            String creditor = expense.getPaidBy().getEmail();
            List<ExpenseShare> shares = expenseShareRepository.findByExpense(expense);

            for (ExpenseShare share : shares) {
                String debtor = share.getUser().getEmail();
                if (!debtor.equals(creditor)) {
                    owesMap.putIfAbsent(debtor, new HashMap<>());
                    BigDecimal currentDebt = owesMap.get(debtor).getOrDefault(creditor, BigDecimal.ZERO);
                    owesMap.get(debtor).put(creditor, currentDebt.add(share.getAmount()));
                }
            }
        }

        List<Settlement> settlements = settlementRepository.findByGroup(group);
        for (Settlement settlement : settlements) {
            String debtor = settlement.getFromUser().getEmail();
            String creditor = settlement.getToUser().getEmail();
            BigDecimal amount = settlement.getAmount();

            owesMap.putIfAbsent(debtor, new HashMap<>());
            BigDecimal currentDebt = owesMap.get(debtor).getOrDefault(creditor, BigDecimal.ZERO);
            owesMap.get(debtor).put(creditor, currentDebt.subtract(amount));
        }

        // 2. Consolidate these pairwise balances for the logged-in user
        Map<String, BigDecimal> myPairwiseBalances = new HashMap<>();

        // Debts I owe to others
        if (owesMap.containsKey(userEmail)) {
            for (Map.Entry<String, BigDecimal> entry : owesMap.get(userEmail).entrySet()) {
                String creditor = entry.getKey();
                BigDecimal amountIOwe = entry.getValue();
                myPairwiseBalances.put(creditor,
                        myPairwiseBalances.getOrDefault(creditor, BigDecimal.ZERO).subtract(amountIOwe));
            }
        }

        // Debts others owe to me
        for (Map.Entry<String, Map<String, BigDecimal>> debtorEntry : owesMap.entrySet()) {
            String debtor = debtorEntry.getKey();
            if (!debtor.equals(userEmail)) {
                BigDecimal amountTheyOweMe = debtorEntry.getValue().getOrDefault(userEmail, BigDecimal.ZERO);
                myPairwiseBalances.put(debtor,
                        myPairwiseBalances.getOrDefault(debtor, BigDecimal.ZERO).add(amountTheyOweMe));
            }
        }

        // 3. Build the response object
        BalanceDto.GroupBalanceResponse response = new BalanceDto.GroupBalanceResponse();
        BigDecimal myNetBalance = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : myPairwiseBalances.entrySet()) {
            String otherUserEmail = entry.getKey();
            BigDecimal netAmount = entry.getValue();

            myNetBalance = myNetBalance.add(netAmount);

            // Skip users where the balance is exactly zero
            if (netAmount.compareTo(BigDecimal.ZERO) == 0)
                continue;

            User otherUser = userService.getByEmail(otherUserEmail);

            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Positive means they owe me
                response.getYouOweMe().add(new BalanceDto.BalanceDetail(
                        otherUser.getEmail(),
                        otherUser.getName(),
                        netAmount));
            } else {
                // Negative means I owe them (flip to positive for UI display)
                response.getIOweThem().add(new BalanceDto.BalanceDetail(
                        otherUser.getEmail(),
                        otherUser.getName(),
                        netAmount.abs()));
            }
        }

        response.setMyBalance(myNetBalance);
        return response;
    }

    public boolean canDeleteUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        List<Group> groups = groupRepository.findByMembersContaining(user);

        if (groups == null || groups.isEmpty()) {
            return true;
        }

        for (Group group : groups) {
            BalanceDto.GroupBalanceResponse balance = getGroupBalanceForUser(group.getId(), user.getEmail());
            if (balance.getMyBalance().compareTo(BigDecimal.ZERO) != 0) {
                return false;
            }
            if (!balance.getIOweThem().isEmpty() || !balance.getYouOweMe().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    // Debug method to check expense shares directly
    // public Map<String, Object> debugExpenseShares(Long groupId) {
    //     Group group = groupService.getById(groupId);
    //     Map<String, Object> debug = new HashMap<>();

    //     List<Expense> expenses = expenseRepository.findByGroup(group);
    //     List<Map<String, Object>> expenseDetails = new ArrayList<>();

    //     for (Expense expense : expenses) {
    //         Map<String, Object> expenseMap = new HashMap<>();
    //         expenseMap.put("expense_id", expense.getId());
    //         expenseMap.put("amount", expense.getAmount());
    //         expenseMap.put("paid_by", expense.getPaidBy().getEmail());

    //         List<ExpenseShare> shares = expenseShareRepository.findByExpense(expense);
    //         List<Map<String, Object>> shareDetails = new ArrayList<>();

    //         for (ExpenseShare share : shares) {
    //             Map<String, Object> shareMap = new HashMap<>();
    //             shareMap.put("user", share.getUser().getEmail());
    //             shareMap.put("amount", share.getAmount());
    //             shareDetails.add(shareMap);
    //         }

    //         expenseMap.put("shares", shareDetails);
    //         expenseDetails.add(expenseMap);
    //     }

    //     debug.put("expenses", expenseDetails);
    //     debug.put("total_expenses", expenses.size());

    //     return debug;
    // }
}