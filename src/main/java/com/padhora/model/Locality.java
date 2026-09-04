package com.padhora.model;

import jakarta.persistence.*;
import java.time.Instant;

// A named Tricity locality a parent can search from. Seeded by name in migration V4;
// coordinates are filled in afterwards by LocalityGeocodingService.
//
// latitude/longitude nullable is load-bearing: a guessed coordinate would silently
// misrank "nearest tutor" with no way to tell which row was wrong, so an un-geocoded
// locality is honestly marked as such rather than given a plausible default.
@Entity
@Table(name = "localities")
public class Locality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String city;

    private Double latitude;
    private Double longitude;

    @Column(name = "geocoded_at")
    private Instant geocodedAt;

    // Why a row is still un-geocoded, when we know: "no result from Places", "ambiguous",
    // and so on. Left for a human to read, not parsed by anything.
    @Column(name = "geocode_note", length = 255)
    private String geocodeNote;

    @Column(nullable = false)
    private Boolean active = true;

    public boolean isGeocoded() { return latitude != null && longitude != null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Instant getGeocodedAt() { return geocodedAt; }
    public void setGeocodedAt(Instant geocodedAt) { this.geocodedAt = geocodedAt; }
    public String getGeocodeNote() { return geocodeNote; }
    public void setGeocodeNote(String geocodeNote) { this.geocodeNote = geocodeNote; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
