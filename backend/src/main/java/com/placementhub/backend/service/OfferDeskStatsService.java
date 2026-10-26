package com.placementhub.backend.service;

import com.placementhub.backend.dto.OfferDeskResponse;
import com.placementhub.backend.dto.PlacementOverviewResponse;
import com.placementhub.backend.enums.JobTier;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only numbers for the Offer Desk dashboards (phase 4).
 *
 * Plain SQL aggregates (COUNT / SUM / GROUP BY) rather than loading entities: each
 * dashboard is a fixed handful of queries however many jobs or applications there are.
 * The seat counters are read straight from the jobs row, the same numbers the atomic
 * UPDATEs maintain.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferDeskStatsService {

    /** Offer outcomes once decided: accepted (incl. later released), declined, expired. */
    private static final String DECIDED = "('ACCEPTED', 'RELEASED', 'DECLINED', 'EXPIRED')";
    private static final String AUTO_REFILL_REMARK = "Offered automatically from the waitlist%";
    private static final int RECENT_EVENTS = 15;

    private final JdbcTemplate jdbc;

    public OfferDeskResponse offerDesk(Long recruiterId) {
        Map<Long, List<OfferDeskResponse.PendingOffer>> offersByJob = new HashMap<>();
        jdbc.query("""
                SELECT a.job_id, a.id, s.full_name, a.offer_expires_at
                FROM applications a
                JOIN jobs j ON j.id = a.job_id
                JOIN students s ON s.id = a.student_id
                WHERE j.recruiter_id = ? AND a.status = 'OFFERED'
                ORDER BY a.offer_expires_at""", rs -> {
            offersByJob.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                    .add(new OfferDeskResponse.PendingOffer(rs.getLong(2), rs.getString(3), toLocal(rs.getTimestamp(4))));
        }, recruiterId);

        List<OfferDeskResponse.JobDesk> jobs = jdbc.query("""
                SELECT j.id, j.title, j.tier, j.status, j.openings, j.offers_accepted, j.offers_pending,
                       (SELECT COUNT(*) FROM applications w WHERE w.job_id = j.id AND w.status = 'WAITLISTED')
                FROM jobs j
                WHERE j.recruiter_id = ?
                ORDER BY j.posted_at DESC, j.id DESC""",
                (rs, n) -> new OfferDeskResponse.JobDesk(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8),
                        offersByJob.getOrDefault(rs.getLong(1), List.of())),
                recruiterId);

        Map<String, Long> decided = countByStatus("""
                SELECT a.status, COUNT(*) FROM applications a JOIN jobs j ON j.id = a.job_id
                WHERE j.recruiter_id = ? AND a.status IN """ + DECIDED + " GROUP BY a.status", recruiterId);
        Long autoRefills = jdbc.queryForObject("""
                SELECT COUNT(*) FROM application_timeline t
                JOIN applications a ON a.id = t.application_id JOIN jobs j ON j.id = a.job_id
                WHERE j.recruiter_id = ? AND t.status = 'OFFERED' AND t.remarks LIKE ?""",
                Long.class, recruiterId, AUTO_REFILL_REMARK);

        List<OfferDeskResponse.Event> events = jdbc.query("""
                SELECT t.application_id, j.title, s.full_name, t.status, t.remarks, t.changed_at
                FROM application_timeline t
                JOIN applications a ON a.id = t.application_id
                JOIN jobs j ON j.id = a.job_id
                JOIN students s ON s.id = a.student_id
                WHERE j.recruiter_id = ?
                  AND t.status IN ('WAITLISTED', 'OFFERED', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'RELEASED', 'WITHDRAWN')
                ORDER BY t.changed_at DESC, t.id DESC
                LIMIT ?""",
                (rs, n) -> new OfferDeskResponse.Event(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), toLocal(rs.getTimestamp(6))),
                recruiterId, RECENT_EVENTS);

        OfferDeskResponse.Totals totals = new OfferDeskResponse.Totals(
                jobs.stream().mapToInt(OfferDeskResponse.JobDesk::openings).sum(),
                jobs.stream().mapToInt(OfferDeskResponse.JobDesk::filled).sum(),
                jobs.stream().mapToInt(OfferDeskResponse.JobDesk::pendingOffers).sum(),
                jobs.stream().mapToInt(OfferDeskResponse.JobDesk::waitlisted).sum(),
                acceptanceRate(decided),
                autoRefills == null ? 0 : autoRefills.intValue());
        return new OfferDeskResponse(totals, jobs, events);
    }

    public PlacementOverviewResponse placementOverview() {
        long students = count("SELECT COUNT(*) FROM students");
        long placed = count("SELECT COUNT(*) FROM students WHERE placed_application_id IS NOT NULL");

        Map<String, Long> placedByTier = new LinkedHashMap<>();
        for (JobTier tier : JobTier.values()) {
            placedByTier.put(tier.name(), 0L);
        }
        jdbc.query("SELECT placed_tier, COUNT(*) FROM students WHERE placed_tier > 0 GROUP BY placed_tier", rs -> {
            for (JobTier tier : JobTier.values()) {
                if (tier.rank() == rs.getInt(1)) {
                    placedByTier.put(tier.name(), rs.getLong(2));
                }
            }
        });

        Map<String, Long> offers = new LinkedHashMap<>();
        for (String status : List.of("OFFERED", "ACCEPTED", "DECLINED", "EXPIRED", "RELEASED", "WITHDRAWN")) {
            offers.put(status, 0L);
        }
        offers.putAll(countByStatus("""
                SELECT status, COUNT(*) FROM applications
                WHERE status IN ('OFFERED', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'RELEASED', 'WITHDRAWN')
                GROUP BY status"""));

        // An application is offered at most once, so each decision pairs with one OFFERED row.
        // Offers recorded as accepted by the V5 migration have no OFFERED row and are skipped.
        Double avgMinutes = jdbc.queryForObject("""
                SELECT AVG(TIMESTAMPDIFF(MINUTE, o.changed_at, d.changed_at))
                FROM application_timeline d
                JOIN application_timeline o ON o.application_id = d.application_id AND o.status = 'OFFERED'
                WHERE d.status IN ('ACCEPTED', 'DECLINED') AND o.changed_at <= d.changed_at""", Double.class);

        Map<String, Object> seats = jdbc.queryForMap("""
                SELECT COALESCE(SUM(openings), 0) AS openings, COALESCE(SUM(offers_accepted), 0) AS filled
                FROM jobs WHERE status IN ('APPROVED', 'CLOSED')""");

        List<PlacementOverviewResponse.CompanyHires> top = jdbc.query("""
                SELECT c.name, COUNT(*) AS hires
                FROM applications a JOIN jobs j ON j.id = a.job_id JOIN companies c ON c.id = j.company_id
                WHERE a.status = 'ACCEPTED'
                GROUP BY c.id, c.name
                ORDER BY hires DESC, c.name
                LIMIT 5""", (rs, n) -> new PlacementOverviewResponse.CompanyHires(rs.getString(1), rs.getLong(2)));

        return new PlacementOverviewResponse(students, placed, students == 0 ? null : (double) placed / students,
                placedByTier, offers, acceptanceRate(offers),
                avgMinutes == null ? null : Math.round(avgMinutes / 6.0) / 10.0,
                ((Number) seats.get("openings")).longValue(), ((Number) seats.get("filled")).longValue(),
                count("SELECT COUNT(*) FROM application_timeline WHERE status = 'OFFERED' AND remarks LIKE ?", AUTO_REFILL_REMARK),
                top);
    }

    private static Double acceptanceRate(Map<String, Long> byStatus) {
        long accepted = byStatus.getOrDefault("ACCEPTED", 0L) + byStatus.getOrDefault("RELEASED", 0L);
        long decided = accepted + byStatus.getOrDefault("DECLINED", 0L) + byStatus.getOrDefault("EXPIRED", 0L);
        return decided == 0 ? null : (double) accepted / decided;
    }

    private Map<String, Long> countByStatus(String sql, Object... args) {
        return jdbc.query(sql, (rs, n) -> Map.entry(rs.getString(1), rs.getLong(2)), args).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private static LocalDateTime toLocal(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
