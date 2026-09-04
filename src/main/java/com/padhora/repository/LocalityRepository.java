package com.padhora.repository;

import com.padhora.model.Locality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocalityRepository extends JpaRepository<Locality, Long> {

    List<Locality> findByActiveTrueOrderByCityAscNameAsc();

    // Localities still waiting on the geocoding pass. These are usable as labels and as
    // autocomplete entries, but cannot take part in distance ranking yet.
    List<Locality> findByLatitudeIsNullAndActiveTrue();

    long countByLatitudeIsNotNull();

    List<Locality> findByCityAndActiveTrueOrderByNameAsc(String city);
}
