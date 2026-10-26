package com.placementhub.backend.repository;

import com.placementhub.backend.entity.Application;
import com.placementhub.backend.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // List endpoints map job title and company name, so fetch both in the same query.
    @EntityGraph(attributePaths = {"job", "job.company"})
    List<Application> findByStudentId(Long studentId);

    // Recruiters also see who applied, so the student and their user row come along too.
    @EntityGraph(attributePaths = {"job", "job.company", "student", "student.user"})
    List<Application> findByJobIdOrderByAppliedAtAsc(Long jobId);

    boolean existsByStudentIdAndJobId(Long studentId, Long jobId);
    boolean existsByJobId(Long jobId);
    long countByStatus(ApplicationStatus status);

    /** Offers whose deadline has passed, oldest first (uses idx_applications_offer_expiry). */
    @Query("select a.id from Application a where a.status = com.placementhub.backend.enums.ApplicationStatus.OFFERED "
            + "and a.offerExpiresAt <= :now order by a.offerExpiresAt")
    List<Long> findDueOfferIds(@Param("now") LocalDateTime now, Pageable limit);

    // ---- Waitlist (Offer Desk phase 2)

    /** A job's waitlist, next in line first (equal ranks: earliest application first). */
    @Query("select a from Application a where a.job.id = :jobId "
            + "and a.status = com.placementhub.backend.enums.ApplicationStatus.WAITLISTED "
            + "order by a.waitlistRank, a.id")
    List<Application> findWaitlist(@Param("jobId") Long jobId);

    @Query("select coalesce(max(a.waitlistRank), 0) from Application a where a.job.id = :jobId "
            + "and a.status = com.placementhub.backend.enums.ApplicationStatus.WAITLISTED")
    int maxWaitlistRank(@Param("jobId") Long jobId);

    /** How many are ahead of this rank on the job's waitlist (position = this + 1). */
    @Query("select count(a) from Application a where a.job.id = :jobId "
            + "and a.status = com.placementhub.backend.enums.ApplicationStatus.WAITLISTED "
            + "and (a.waitlistRank < :rank or (a.waitlistRank = :rank and a.id < :id))")
    long countAheadOnWaitlist(@Param("jobId") Long jobId, @Param("rank") int rank, @Param("id") Long applicationId);

    /** Jobs where this student is waiting, to retry them when the student becomes free. */
    @Query("select a.job.id from Application a where a.student.id = :studentId "
            + "and a.status = com.placementhub.backend.enums.ApplicationStatus.WAITLISTED")
    List<Long> findJobIdsWaitlisting(@Param("studentId") Long studentId);

    /**
     * Turns a waitlisted application into an offer, only if it is still WAITLISTED. Atomic,
     * so two promotions racing for the same candidate cannot both succeed (0 = lost the race).
     * Bumps the @Version like an entity update would.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Application a set a.status = com.placementhub.backend.enums.ApplicationStatus.OFFERED, "
            + "a.offerExpiresAt = :expiresAt, a.waitlistRank = null, a.updatedAt = :now, a.version = a.version + 1 "
            + "where a.id = :id and a.status = com.placementhub.backend.enums.ApplicationStatus.WAITLISTED")
    int promoteFromWaitlist(@Param("id") Long applicationId, @Param("expiresAt") LocalDateTime expiresAt,
                            @Param("now") LocalDateTime now);
}
