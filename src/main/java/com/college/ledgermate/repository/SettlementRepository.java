package com.college.ledgermate.repository;

import com.college.ledgermate.model.Group;
import com.college.ledgermate.model.Settlement;
import com.college.ledgermate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByGroup(Group group);

    @Query("SELECT s FROM Settlement s WHERE s.group.id = :groupId")
    List<Settlement> findByGroupId(@Param("groupId") Long groupId);

    // Money the user has sent to others
    @Query("SELECT s FROM Settlement s WHERE s.fromUser = :user")
    List<Settlement> findSettlementsSentByUser(@Param("user") User user);

    // Money the user has received from others
    @Query("SELECT s FROM Settlement s WHERE s.toUser = :user")
    List<Settlement> findSettlementsReceivedByUser(@Param("user") User user);

    // Get settlements for a specific user in a specific group
    @Query("SELECT s FROM Settlement s WHERE s.group = :group AND (s.fromUser = :user OR s.toUser = :user)")
    List<Settlement> findByGroupAndUser(@Param("group") Group group, @Param("user") User user);
}