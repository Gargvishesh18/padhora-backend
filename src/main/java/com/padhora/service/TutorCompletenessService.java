package com.padhora.service;

import com.padhora.model.Tutor;
import org.springframework.stereotype.Service;

/**
 * Computes {@link Tutor#getCompletenessScore()}, the 0-100 value that feeds the 0.10
 * completeness term of the published search ranking:
 *
 * <pre>0.50 * proximity + 0.20 * verified + 0.20 * response_rate + 0.10 * completeness</pre>
 *
 * <p>Called at write time (tutor submits or edits their profile) rather than at search time,
 * so it is a plain column search can sort on directly instead of a per-request computation
 * over every candidate.
 *
 * <p>Each field below is worth an equal share. That is a starting judgment, not a law -
 * change the weights here if a field turns out to matter more or less than the others, but
 * change them here, in the one place the formula lives, not by duplicating logic elsewhere.
 */
@Service
public class TutorCompletenessService {

    // Keep this list and CHECKS.length in agreement - see the loop below.
    private static final int CHECKS = 10;

    public int score(Tutor t) {
        int filled = 0;

        if (hasText(t.getBio())) filled++;
        if (hasText(t.getQualification())) filled++;
        if (hasText(t.getPhotoUrl())) filled++;
        if (hasText(t.getVideoUrl())) filled++;
        if (t.getYearsExperience() != null && t.getYearsExperience() > 0) filled++;
        if (!isEmpty(t.getPreferredTimings())) filled++;
        if (!isEmpty(t.getLanguages())) filled++;
        if (!t.getSubjects().isEmpty()) filled++;
        if (!t.getGrades().isEmpty()) filled++;
        // Either the legacy single price or the newer fee range counts - a tutor should not
        // be penalised for having filled in one and not the other.
        if (t.getPrice() != null || t.getFeeMin() != null || t.getFeeMax() != null) filled++;

        return Math.round(100f * filled / CHECKS);
    }

    private boolean hasText(String s) { return s != null && !s.isBlank(); }
    private boolean isEmpty(java.util.Collection<?> c) { return c == null || c.isEmpty(); }
}
