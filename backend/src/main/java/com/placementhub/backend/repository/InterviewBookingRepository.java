package com.placementhub.backend.repository;

import com.placementhub.backend.entity.InterviewBooking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InterviewBookingRepository extends JpaRepository<InterviewBooking, Long> {

    @EntityGraph(attributePaths = "slot")
    Optional<InterviewBooking> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);

    boolean existsBySlotId(Long slotId);

    /** Bookings for a list of applications in one query (see ApplicationMapper). */
    @EntityGraph(attributePaths = "slot")
    List<InterviewBooking> findByApplicationIdIn(Collection<Long> applicationIds);

    /** Who booked which slot, for the recruiter's slot list. */
    @EntityGraph(attributePaths = {"application", "application.student"})
    List<InterviewBooking> findBySlotIdIn(Collection<Long> slotIds);
}
