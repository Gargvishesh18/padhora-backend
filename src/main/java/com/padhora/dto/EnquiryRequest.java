package com.padhora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EnquiryRequest {

    @NotNull(message = "tutorId is required")
    private Long tutorId;

    @NotBlank(message = "Your name is required")
    @Size(max = 120)
    private String parentName;

    @NotBlank(message = "Your phone number is required")
    private String parentPhone;

    @Size(max = 100) private String className;
    @Size(max = 100) private String subject;
    @Size(max = 100) private String mode;
    @Size(max = 150) private String locality;
    @Size(max = 150) private String preferredTiming;
    @Size(max = 100) private String budget;

    @Size(max = 1000, message = "That message is a bit too long - please keep it under 1000 characters.")
    private String message;

    public Long getTutorId() { return tutorId; }
    public void setTutorId(Long tutorId) { this.tutorId = tutorId; }
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
}
