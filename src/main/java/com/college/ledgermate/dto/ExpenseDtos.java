package com.college.ledgermate.dto;

import com.college.ledgermate.service.ExpenseService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ExpenseDtos {

    @Data
    public static class AddExpenseRequest {
        @NotBlank
        private String description;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal amount;

        @NotBlank
        private String paidByEmail;

        @NotNull
        private ExpenseService.SplitType splitType;

        @NotEmpty
        private List<String> participantEmails;

        // For CUSTOM split: map of email -> amount
        private Map<String, BigDecimal> customSplits;
    }

    @Data
    public static class SettlementRequest {
        @NotBlank
        private String toEmail;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal amount;
    }
}