package com.placementhub.backend.repository;

import com.placementhub.backend.entity.InterviewSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, Long> {

    List<InterviewSlot> findByJobIdOrderByStartsAtAsc(Long jobId);

    boolean existsByJobId(Long jobId);

    void deleteByJobId(Long jobId);

    /**
     * Takes one seat if any is left, as a single atomic statement. MySQL locks the row,
     * re-checks booked_count < capacity and increments it, so two concurrent bookings can
     * never both take the last seat. Returns 1 if a seat was taken, 0 if the slot is full.
     */
    @Modifying(flushAutomatically = true)
    @Query("update InterviewSlot s set s.bookedCount = s.bookedCount + 1 "
            + "where s.id = :id and s.bookedCount < s.capacity")
    int reserveSeat(@Param("id") Long slotId);

    /** Gives a seat back (booking cancelled). Never goes below zero. */
    @Modifying(flushAutomatically = true)
    @Query("update InterviewSlot s set s.bookedCount = s.bookedCount - 1 "
            + "where s.id = :id and s.bookedCount > 0")
    int releaseSeat(@Param("id") Long slotId);
}
