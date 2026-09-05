package com.padhora.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tutors")
public class Tutor {

    // PAUSED: the tutor is full or away. Their listing stops appearing in search, but
    // nothing is deleted and they are not "rejected" - they flip it back themselves.
    public enum Status { DRAFT, PENDING, APPROVED, REJECTED, PAUSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Login credentials - present for tutors who signed up via the auth flow.
    // Nullable because older listings (submitted before auth existed) have neither.
    @Column(unique = true)
    private String email;

    @JsonIgnore
    private String passwordHash;

    private String name;

    private String phone;

    private String area;

    // Specific locality/sector within the area, e.g. "Phase 5, Mohali" - from Places autocomplete
    private String locality;

    private Double latitude;
    private Double longitude;

    @JsonIgnore
    @Column(length = 500)
    private String fullAddress;

    @ElementCollection
    @CollectionTable(name = "tutor_modes", joinColumns = @JoinColumn(name = "tutor_id"))
    @Column(name = "mode")
    private List<String> modes;

    @ElementCollection
    @CollectionTable(name = "tutor_tuition_types", joinColumns = @JoinColumn(name = "tutor_id"))
    @Column(name = "tuition_type")
    private List<String> types;

    // Free-text "Class 6 - Class 8" -> "Math, Science". Unsearchable, and the reason the
    // site can filter by help-type and mode but not by subject or grade. Superseded by the
    // normalised `subjects`/`grades` below, which migration V7 backfilled from this. Kept
    // because it is still what the live listing renders; Phase 2 moves reads across and a
    // later migration retires it.
    @ElementCollection
    @CollectionTable(name = "tutor_grade_subjects", joinColumns = @JoinColumn(name = "tutor_id"))
    private List<GradeSubjects> gradeSubjects;

    // Not serialized yet. Phase 1 is the data model; Phase 2 exposes these through a DTO
    // with an explicit fetch join. Serializing a lazy association straight out of the
    // entity would quietly turn one search into an N+1 query storm.
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tutor_subjects",
            joinColumns = @JoinColumn(name = "tutor_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id"))
    private Set<Subject> subjects = new LinkedHashSet<>();

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tutor_grades",
            joinColumns = @JoinColumn(name = "tutor_id"),
            inverseJoinColumns = @JoinColumn(name = "grade_id"))
    private Set<Grade> grades = new LinkedHashSet<>();

    // The seeded locality this tutor teaches in. `locality` above stays as the free-text
    // label the tutor typed; this is the one distance search can rely on.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locality_id")
    private Locality localityRef;

    private Integer feeMin;
    private Integer feeMax;

    // 0-100, feeds the 0.10 completeness term of the published ranking. Computed and
    // persisted by TutorCompletenessService whenever a tutor submits or edits their
    // profile - see that class for what counts toward it.
    @JsonIgnore
    @Column(nullable = false)
    private Integer completenessScore = 0;

    // 0.0-1.0, or null for "not asked yet". Null is not zero: a tutor nobody has contacted
    // has not failed to respond, and must not be ranked as though they had.
    @JsonIgnore
    private Double responseRate;

    // The public "Verified" badge renders only when this is set. Being APPROVED is not the
    // same thing as being verified.
    private Instant verifiedAt;

    // Filled in by TutorSearchService for a specific search request, never persisted. Lets a
    // search response tell a parent "3.2 km away" using the exact number that decided the
    // sort order, instead of the frontend re-deriving it (and risking a different answer).
    @Transient
    private Double distanceKm;

    @Transient
    private Double rankScore;

    private String priceType;
    private Integer price;
    private String priceUnit;
    private String batchType;
    private Boolean trialAvailable;

    @ElementCollection
    @CollectionTable(name = "tutor_timings", joinColumns = @JoinColumn(name = "tutor_id"))
    @Column(name = "timing")
    private List<String> preferredTimings;

    private String qualification;

    @ElementCollection
    @CollectionTable(name = "tutor_languages", joinColumns = @JoinColumn(name = "tutor_id"))
    @Column(name = "language")
    private List<String> languages;

    @Column(length = 1000)
    private String bio;

    private String photoUrl;
    private String videoUrl;

    private Integer yearsExperience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getFullAddress() { return fullAddress; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
    public List<String> getModes() { return modes; }
    public void setModes(List<String> modes) { this.modes = modes; }
    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }
    public List<GradeSubjects> getGradeSubjects() { return gradeSubjects; }
    public void setGradeSubjects(List<GradeSubjects> gradeSubjects) { this.gradeSubjects = gradeSubjects; }
    public String getPriceType() { return priceType; }
    public void setPriceType(String priceType) { this.priceType = priceType; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    public String getBatchType() { return batchType; }
    public void setBatchType(String batchType) { this.batchType = batchType; }
    public Boolean getTrialAvailable() { return trialAvailable; }
    public void setTrialAvailable(Boolean trialAvailable) { this.trialAvailable = trialAvailable; }
    public List<String> getPreferredTimings() { return preferredTimings; }
    public void setPreferredTimings(List<String> preferredTimings) { this.preferredTimings = preferredTimings; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public Integer getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Set<Subject> getSubjects() { return subjects; }
    public void setSubjects(Set<Subject> subjects) { this.subjects = subjects; }
    public Set<Grade> getGrades() { return grades; }
    public void setGrades(Set<Grade> grades) { this.grades = grades; }
    public Locality getLocalityRef() { return localityRef; }
    public void setLocalityRef(Locality localityRef) { this.localityRef = localityRef; }
    public Integer getFeeMin() { return feeMin; }
    public void setFeeMin(Integer feeMin) { this.feeMin = feeMin; }
    public Integer getFeeMax() { return feeMax; }
    public void setFeeMax(Integer feeMax) { this.feeMax = feeMax; }
    public Integer getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(Integer completenessScore) { this.completenessScore = completenessScore; }
    public Double getResponseRate() { return responseRate; }
    public void setResponseRate(Double responseRate) { this.responseRate = responseRate; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    @JsonIgnore
    public Double getRankScore() { return rankScore; }
    public void setRankScore(Double rankScore) { this.rankScore = rankScore; }

    // The badge is a promise, so it is tied to a verification record existing - never to
    // status alone.
    public boolean isVerified() { return verifiedAt != null; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
