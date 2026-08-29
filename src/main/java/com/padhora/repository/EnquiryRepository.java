package com.padhora.repository;

import com.padhora.model.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    Optional<Enquiry> findByPublicToken(String publicToken);

    // Tutor's inbox - newest first, capped so a high-volume tutor's dashboard doesn't
    // eventually load an unbounded list.
    List<Enquiry> findTop200ByTutorIdOrderByCreatedAtDesc(Long tutorId);

    // Parent's "My Requests" - everything filed under a phone number, across however many kids.
    List<Enquiry> findTop100ByParentPhoneOrderByCreatedAtDesc(String parentPhone);

    // Duplicate-submission guard: same parent hitting the same tutor again within a short window
    // is almost certainly a double-click/retry, not a second genuine request.
    Optional<Enquiry> findFirstByTutorIdAndParentPhoneAndCreatedAtAfterOrderByCreatedAtDesc(
            Long tutorId, String parentPhone, Instant after);
}
