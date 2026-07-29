package com.college.ledgermate.repository;

import com.college.ledgermate.model.Expense;
import com.college.ledgermate.model.ExpenseShare;
import com.college.ledgermate.model.Group;
import com.college.ledgermate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {
    List<ExpenseShare> findByExpense(Expense expense);
    List<ExpenseShare> findByUser(User user);
    List<ExpenseShare> findByExpenseAndUser(Expense expense, User user);

    @Query("SELECT es FROM ExpenseShare es WHERE es.expense.group = :group AND es.user = :user")
    List<ExpenseShare> findByGroupAndUser(@Param("group") Group group, @Param("user") User user);

    @Query("SELECT SUM(es.amount) FROM ExpenseShare es WHERE es.expense.group = :group AND es.user = :user")
    BigDecimal getTotalSharesForUserInGroup(@Param("group") Group group, @Param("user") User user);
}