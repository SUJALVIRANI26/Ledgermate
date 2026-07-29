package com.college.ledgermate.controller;

import com.college.ledgermate.dto.BalanceDto;
import com.college.ledgermate.dto.ExpenseDtos;
import com.college.ledgermate.model.Expense;
import com.college.ledgermate.model.Settlement;
import com.college.ledgermate.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{groupId}")
    public ResponseEntity<Expense> addExpense(
            @PathVariable Long groupId,
            @Valid @RequestBody ExpenseDtos.AddExpenseRequest request) {

        Expense expense = expenseService.addExpense(
                groupId,
                request.getDescription(),
                request.getAmount(),
                request.getPaidByEmail(),
                request.getSplitType(),
                request.getParticipantEmails(),
                request.getCustomSplits()
        );
        return ResponseEntity.ok(expense);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/group/{groupId}/settle")
    public ResponseEntity<Settlement> recordSettlement(
            @PathVariable Long groupId,
            @Valid @RequestBody ExpenseDtos.SettlementRequest request,
            Authentication authentication) {

        String fromEmail = authentication.getName();
        Settlement settlement = expenseService.recordSettlement(
                groupId,
                fromEmail,
                request.getToEmail(),
                request.getAmount()
        );
        return ResponseEntity.ok(settlement);
    }

    // Main balance endpoint - Shows simplified balance for the logged-in user in a group
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/group/{groupId}/balance")
    public ResponseEntity<BalanceDto.GroupBalanceResponse> getGroupBalance(
            @PathVariable Long groupId,
            Authentication authentication) {

        String myEmail = authentication.getName();
        BalanceDto.GroupBalanceResponse balance = expenseService.getGroupBalanceForUser(groupId, myEmail);
        return ResponseEntity.ok(balance);
    }

//    @PreAuthorize("hasRole('USER')")
//    @GetMapping("/group/{groupId}/debug")
//    public ResponseEntity<Map<String, Object>> debugGroup(@PathVariable Long groupId) {
//        Map<String, Object> debug = expenseService.debugExpenseShares(groupId);
//        return ResponseEntity.ok(debug);
//    }
}