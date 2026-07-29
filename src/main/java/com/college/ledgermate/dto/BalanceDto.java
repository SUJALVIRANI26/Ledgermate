package com.college.ledgermate.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BalanceDto {

    // Simplified group balance response
    public static class GroupBalanceResponse {
        private BigDecimal myBalance;  // Positive = I'm owed, Negative = I owe
        private List<BalanceDetail> youOweMe = new ArrayList<>();  // People who owe me
        private List<BalanceDetail> iOweThem = new ArrayList<>();  // People I owe

        public BigDecimal getMyBalance() {
            return myBalance;
        }

        public void setMyBalance(BigDecimal myBalance) {
            this.myBalance = myBalance;
        }

        public List<BalanceDetail> getYouOweMe() {
            return youOweMe;
        }

        public void setYouOweMe(List<BalanceDetail> youOweMe) {
            this.youOweMe = youOweMe;
        }

        public List<BalanceDetail> getIOweThem() {
            return iOweThem;
        }

        public void setIOweThem(List<BalanceDetail> iOweThem) {
            this.iOweThem = iOweThem;
        }
    }

    public static class BalanceDetail {
        private String email;
        private String name;
        private BigDecimal amount;

        public BalanceDetail() {}

        public BalanceDetail(String email, String name, BigDecimal amount) {
            this.email = email;
            this.name = name;
            this.amount = amount;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}