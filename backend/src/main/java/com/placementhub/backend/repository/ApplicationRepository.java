package com.placementhub.backend.repository;

import com.placementhub.backend.entity.Application;
import com.placementhub.backend.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
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
}
