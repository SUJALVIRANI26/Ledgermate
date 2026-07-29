package com.college.ledgermate.repository;

import com.college.ledgermate.model.Expense;
import com.college.ledgermate.model.Group;
import com.college.ledgermate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroup(Group group);

    List<Expense> findByPaidBy(User user);

}
