package com.padhora.dto;

import com.padhora.model.GradeSubjects;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class TutorRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Area is required")
    private String area;

    private String locality;
    private Double latitude;
    private Double longitude;

    private String fullAddress;

    @NotEmpty(message = "At least one mode is required")
    private List<String> modes;

    @NotEmpty(message = "At least one tuition type is required")
    private List<String> types;

    private List<GradeSubjects> gradeSubjects;

    // Structured teaching scope, by slug (see GET /api/grades, GET /api/subjects). Optional
    // for now so older/unmigrated callers keep working, but a tutor submitted without these
    // will not appear in a grade- or subject-filtered search - only in the legacy free-text
    // display. dashboard.html sends these; keep both in sync if you touch the signup flow.
    private List<String> gradeSlugs;
    private List<String> subjectSlugs;

    private String priceType;
    private Integer price;
    private String priceUnit;
    private String batchType;
    private Boolean trialAvailable;
    private List<String> preferredTimings;
    private String qualification;
    private List<String> languages;
    private String bio;
    private String photoUrl;
    private String videoUrl;
    private Integer yearsExperience;

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
    public List<String> getGradeSlugs() { return gradeSlugs; }
    public void setGradeSlugs(List<String> gradeSlugs) { this.gradeSlugs = gradeSlugs; }
    public List<String> getSubjectSlugs() { return subjectSlugs; }
    public void setSubjectSlugs(List<String> subjectSlugs) { this.subjectSlugs = subjectSlugs; }
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
}
