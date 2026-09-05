package com.padhora.repository;

import com.padhora.model.ParentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentRequestRepository extends JpaRepository<ParentRequest, Long> {

    // Admin inbox - newest first, capped so it can't eventually become an unbounded load.
    List<ParentRequest> findTop300ByOrderByCreatedAtDesc();

    List<ParentRequest> findTop300ByStatusOrderByCreatedAtDesc(ParentRequest.Status status);
}
