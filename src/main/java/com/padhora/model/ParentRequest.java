package com.padhora.model;

import jakarta.persistence.*;
import java.time.Instant;

// A parent's unmet demand - submitted when a search turned up no tutor, so it has no tutorId
// at all (unlike Enquiry, which always points at one). Matching this to a tutor is a manual
// admin action (see matchedTutorId), not automated - see migration V9 for why.
//
// Flat / no JPA relations, matching Enquiry's style in this codebase: plain FK columns,
// controllers look up Tutor/Grade/Subject separately when they need more than the id.
@Entity
@Table(name = "parent_requests")
public class ParentRequest {

    public enum Status { NEW, MATCHED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String parentPhone;

    @Column(nullable = false, length = 150)
    private String localityText;

    private Long gradeId;
    private Long subjectId;

    @Column(length = 60)
    private String mode;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NEW;

    private Long matchedTutorId;
    private Instant matchedAt;

    @Column(length = 1000)
    private String adminNotes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }
    public String getLocalityText() { return localityText; }
    public void setLocalityText(String localityText) { this.localityText = localityText; }
    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Long getMatchedTutorId() { return matchedTutorId; }
    public void setMatchedTutorId(Long matchedTutorId) { this.matchedTutorId = matchedTutorId; }
    public Instant getMatchedAt() { return matchedAt; }
    public void setMatchedAt(Instant matchedAt) { this.matchedAt = matchedAt; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
