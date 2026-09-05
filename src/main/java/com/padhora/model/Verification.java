package com.padhora.model;

import jakarta.persistence.*;
import java.time.Instant;

// The evidence behind a tutor's public "Verified" badge.
//
// PRIVACY: this record never holds an ID image or a full ID number. Only the document
// type, its last four digits, and whether the name matched. Storing more would make
// Padhora a custodian of sensitive personal data under the DPDP Act for no product gain -
// the actual check happens when a human looks at the document on the verification call.
// The database enforces the four-digit limit; see V8__verifications.sql.
@Entity
@Table(name = "verifications")
public class Verification {

    public enum IdType { AADHAAR, PAN, VOTER_ID, DRIVING_LICENCE, PASSPORT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tutor_id", nullable = false, unique = true)
    private Long tutorId;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", length = 40)
    private IdType idType;

    // Exactly four digits. Never the full number - the column is too small to hold one and
    // a CHECK constraint rejects anything else.
    @Column(name = "id_last4", length = 4)
    private String idLast4;

    @Column(name = "name_matched")
    private Boolean nameMatched;

    @Column(name = "locality_confirmed", nullable = false)
    private Boolean localityConfirmed = false;

    @Column(name = "call_completed_at")
    private Instant callCompletedAt;

    @Column(name = "call_notes", length = 1000)
    private String callNotes;

    // Set only once every step above is done; the database refuses a partial one.
    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by", length = 120)
    private String verifiedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    // Every check the badge claims. Mirrors ck_verifications_complete so the application
    // can explain what is still outstanding instead of just catching a constraint error.
    public boolean isComplete() {
        return phoneVerifiedAt != null
                && idType != null
                && idLast4 != null
                && Boolean.TRUE.equals(nameMatched)
                && Boolean.TRUE.equals(localityConfirmed)
                && callCompletedAt != null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTutorId() { return tutorId; }
    public void setTutorId(Long tutorId) { this.tutorId = tutorId; }
    public Instant getPhoneVerifiedAt() { return phoneVerifiedAt; }
    public void setPhoneVerifiedAt(Instant phoneVerifiedAt) { this.phoneVerifiedAt = phoneVerifiedAt; }
    public IdType getIdType() { return idType; }
    public void setIdType(IdType idType) { this.idType = idType; }
    public String getIdLast4() { return idLast4; }
    public void setIdLast4(String idLast4) { this.idLast4 = idLast4; }
    public Boolean getNameMatched() { return nameMatched; }
    public void setNameMatched(Boolean nameMatched) { this.nameMatched = nameMatched; }
    public Boolean getLocalityConfirmed() { return localityConfirmed; }
    public void setLocalityConfirmed(Boolean localityConfirmed) { this.localityConfirmed = localityConfirmed; }
    public Instant getCallCompletedAt() { return callCompletedAt; }
    public void setCallCompletedAt(Instant callCompletedAt) { this.callCompletedAt = callCompletedAt; }
    public String getCallNotes() { return callNotes; }
    public void setCallNotes(String callNotes) { this.callNotes = callNotes; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
