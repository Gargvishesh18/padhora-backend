package com.padhora.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "tutors")
public class Tutor {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
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

    @ElementCollection
    @CollectionTable(name = "tutor_grade_subjects", joinColumns = @JoinColumn(name = "tutor_id"))
    private List<GradeSubjects> gradeSubjects;

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
    private Status status = Status.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
