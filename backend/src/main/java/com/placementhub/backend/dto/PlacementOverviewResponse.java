package com.placementhub.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * The placement cell's overview of the season (Offer Desk phase 4).
 *
 * @param placementRate        placed students ÷ students, or null with no students
 * @param placedByTier         REGULAR / DREAM / SUPER_DREAM → students currently placed there
 * @param offers               offer outcomes: OFFERED (pending), ACCEPTED, DECLINED, EXPIRED,
 *                             RELEASED (given up for an upgrade), WITHDRAWN (closed by the rules)
 * @param acceptanceRate       accepted ÷ decided offers, or null before any decision
 * @param avgHoursToDecide     average time from offer to accept/decline, or null
 * @param openings             openings across approved and closed jobs
 * @param filled               of which accepted
 * @param autoRefills          offers made automatically from a waitlist
 * @param topCompanies         companies with the most accepted offers
 */
public record PlacementOverviewResponse(long students, long placedStudents, Double placementRate,
                                        Map<String, Long> placedByTier, Map<String, Long> offers,
                                        Double acceptanceRate, Double avgHoursToDecide,
                                        long openings, long filled, long autoRefills,
                                        List<CompanyHires> topCompanies) {

    public record CompanyHires(String company, long hires) {
    }
}
