package com.padhora.controller;

import com.padhora.model.AnalyticsEvent;
import com.padhora.repository.AnalyticsEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Deliberately tiny: one endpoint, one table, no querying/dashboard built here - just closes
// the gap where "profile viewed" and "request started" were invisible before an Enquiry
// existed. Public + fire-and-forget by design (frontend doesn't wait on or care about the
// response), same trust level as the rest of the read side of this API.
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsEventRepository repository;

    public AnalyticsController(AnalyticsEventRepository repository) {
        this.repository = repository;
    }

    public static class EventRequest {
        public String eventType; // PROFILE_VIEWED | REQUEST_STARTED
        public Long tutorId;
    }

    @PostMapping("/event")
    public ResponseEntity<?> record(@RequestBody EventRequest req) {
        AnalyticsEvent.EventType type;
        try {
            type = AnalyticsEvent.EventType.valueOf(req.eventType);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown eventType"));
        }
        if (req.tutorId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "tutorId is required"));
        }

        AnalyticsEvent event = new AnalyticsEvent();
        event.setEventType(type);
        event.setTutorId(req.tutorId);
        repository.save(event);

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
