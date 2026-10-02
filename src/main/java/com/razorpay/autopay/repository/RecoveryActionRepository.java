package com.razorpay.autopay.repository;

import com.razorpay.autopay.entity.RecoveryAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecoveryActionRepository extends JpaRepository<RecoveryAction, Long> {
    @Query("select action from RecoveryAction action join fetch action.customer order by action.createdAt desc")
    List<RecoveryAction> findAllByOrderByCreatedAtDesc();
}
