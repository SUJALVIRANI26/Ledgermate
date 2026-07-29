package com.college.ledgermate.repository;

import com.college.ledgermate.model.Group;
import com.college.ledgermate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByMembersContaining(User user);
}
