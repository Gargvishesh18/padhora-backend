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

    // Public-facing area/locality (e.g. "Mohali") - shown to parents
    @Column(nullable = false)
    private String area;

    // Full address for internal verification only - never returned in public API responses
    @JsonIgnore
    @Column(length = 500)
    private String fullAddress;

    // Multiple modes allowed: a tutor can teach at their home AND online, etc.
    // Values: "At tutor's home" | "Tutor comes to you" | "Online"
    @ElementCollection
    @CollectionTable(name = "tutor_modes", joinColumns = @JoinColumn(name = "tutor_id"))
    @Column(name = "mode")
    private List<String> modes;

    @ElementCollection
    @CollectionTable(name = "tutor_tuition_types", joinColumns = @JoinColumn(name = "tutor_id"))
    @Column(name = "tuition_type")
    private List<String> types;

    // Structured class -> subjects rows, since different classes often need different subjects
    @ElementCollection
    @CollectionTable(name = "tutor_grade_subjects", joinColumns = @JoinColumn(name = "tutor_id"))
    private List<GradeSubjects> gradeSubjects;

    // "Fixed" | "Starting from" | "Custom quote"
    private String priceType;

    // Only meaningful when priceType is Fixed or Starting from
    private Integer price;

    // e.g. "/month", "/10-day batch"
    private String priceUnit;

    private String qualification;

    @ElementCollection
    @CollectionTable(name = "tutor_languages", joinColumns = @JoinColumn(name = "tutor_id"))
    @Column(name = "language")
    private List<String> languages;

    @Column(length = 1000)
    private String bio;

    private Integer yearsExperience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // --- getters and setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

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

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Integer getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
