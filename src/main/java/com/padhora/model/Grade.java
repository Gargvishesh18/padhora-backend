package com.padhora.model;

import jakarta.persistence.*;

// Reference table, seeded by migration V3. Scope is Nursery to Class 8 (decided 2026-09-04).
@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @Column(nullable = false, unique = true, length = 40)
    private String slug;

    // Ascending teaching order, so "Nursery" sorts before "Class 1" without name parsing.
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
