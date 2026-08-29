package com.padhora.model;

import jakarta.persistence.*;
import java.time.Instant;

// Minimal event log for the two funnel steps that happen BEFORE an Enquiry row exists
// (profile viewed, request form opened) - without this there's no way to measure
// view-to-request or request-to-submit drop-off. Deliberately just one flat table,
// not a general-purpose analytics system.
@Entity
@Table(name = "analytics_events")
public class AnalyticsEvent {

    public enum EventType { PROFILE_VIEWED, REQUEST_STARTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private Long tutorId;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public Long getTutorId() { return tutorId; }
    public void setTutorId(Long tutorId) { this.tutorId = tutorId; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
