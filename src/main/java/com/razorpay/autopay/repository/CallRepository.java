package com.razorpay.autopay.repository;

import com.razorpay.autopay.entity.Call;
import com.razorpay.autopay.enums.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CallRepository extends JpaRepository<Call, Long> {
    @Query("select c from Call c join fetch c.customer order by c.startedAt desc")
    List<Call> findAllByOrderByStartedAtDesc();

    Optional<Call> findByVapiCallId(String vapiCallId);

    Optional<Call> findFirstByCustomer_IdAndStatusInOrderByStartedAtDesc(
            Long customerId, Collection<CallStatus> statuses);
}
