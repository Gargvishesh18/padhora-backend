package com.padhora.repository;

import com.padhora.model.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    Optional<Enquiry> findByPublicToken(String publicToken);

    // Tutor's inbox - newest first.
    List<Enquiry> findByTutorIdOrderByCreatedAtDesc(Long tutorId);

    // Parent's "My Requests" - everything filed under a phone number, across however many kids.
    List<Enquiry> findByParentPhoneOrderByCreatedAtDesc(String parentPhone);
}
