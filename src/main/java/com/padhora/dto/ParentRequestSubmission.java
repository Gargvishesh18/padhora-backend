package com.padhora.dto;

import jakarta.validation.constraints.NotBlank;

// The whole form, per the roadmap: phone, locality, class, subject, mode - and nothing else.
// Only phone and locality are required; class/subject/mode stay optional so a parent who
// doesn't know the exact class yet, or is open on subject/mode, isn't blocked from submitting.
public class ParentRequestSubmission {

    @NotBlank(message = "Please enter your phone number.")
    private String phone;

    @NotBlank(message = "Please enter your locality.")
    private String locality;

    // Slugs, not ids - same convention as TutorRequest's gradeSlugs/subjectSlugs, resolved
    // against the Phase 1 reference tables server-side.
    private String gradeSlug;
    private String subjectSlug;

    private String mode;

    // Carried through silently from the search box's own autocomplete/geolocation, not a
    // field the parent fills in - see ParentRequest for why this is worth keeping.
    private Double latitude;
    private Double longitude;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    public String getGradeSlug() { return gradeSlug; }
    public void setGradeSlug(String gradeSlug) { this.gradeSlug = gradeSlug; }
    public String getSubjectSlug() { return subjectSlug; }
    public void setSubjectSlug(String subjectSlug) { this.subjectSlug = subjectSlug; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
