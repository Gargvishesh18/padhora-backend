package com.padhora.model;

import jakarta.persistence.Embeddable;

// One row of "I teach [grade/class range] → [these subjects]"
// e.g. grade="Class 6 - Class 8", subjects="Math, Science"
@Embeddable
public class GradeSubjects {

    private String grade;
    private String subjects;

    public GradeSubjects() {}

    public GradeSubjects(String grade, String subjects) {
        this.grade = grade;
        this.subjects = subjects;
    }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getSubjects() { return subjects; }
    public void setSubjects(String subjects) { this.subjects = subjects; }
}
