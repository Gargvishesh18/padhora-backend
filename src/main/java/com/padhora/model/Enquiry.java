package com.padhora.model;

import jakarta.persistence.*;
import java.time.Instant;

// A parent's request to a tutor - the thing that replaces "click WhatsApp and vanish".
// Deliberately flat / no JPA relations to Tutor, matching the rest of this codebase's style
// (see TutorRepository - no @ManyToOne anywhere). tutorId is just a plain FK column;
// controllers look the Tutor up separately when they need name/phone.
@Entity
@Table(name = "enquiries")
public class Enquiry {

    public enum Status {
        NEW, VIEWED, ACCEPTED, DECLINED, CONNECTED, TUITION_STARTED, COMPLETED, EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tutorId;

    // Opaque token handed to the parent after submission so the success screen / deep link
    // doesn't expose the sequential DB id. Not a security boundary by itself (see the
    // phone+OTP lookup for that) - just avoids trivial enumeration of "enquiry #42, #43...".
    @Column(nullable = false, unique = true)
    private String publicToken;

    @Column(nullable = false)
    private String parentName;

    // Stored in E.164 (e.g. "+919876543210") - see PhoneUtil.
    @Column(nullable = false)
    private String parentPhone;

    private String className;
    private String subject;
    private String mode;
    private String locality;
    private String preferredTiming;
    private String budget;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NEW;

    private Instant whatsappClickedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTutorId() { return tutorId; }
    public void setTutorId(Long tutorId) { this.tutorId = tutorId; }
    public String getPublicToken() { return publicToken; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    public String getPreferredTiming() { return preferredTiming; }
    public void setPreferredTiming(String preferredTiming) { this.preferredTiming = preferredTiming; }
    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getWhatsappClickedAt() { return whatsappClickedAt; }
    public void setWhatsappClickedAt(Instant whatsappClickedAt) { this.whatsappClickedAt = whatsappClickedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
