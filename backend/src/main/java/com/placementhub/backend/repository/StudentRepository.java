package com.placementhub.backend.repository;

import com.placementhub.backend.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    // Responses include the user's email, so fetch the user row with the student.
    @EntityGraph(attributePaths = "user")
    Optional<Student> findByUser_Email(String email);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Student> findAll(Pageable pageable);

    /**
     * Locks the logged-in student's row until commit, so one student's bookings run one
     * after another. Returns the student's id.
     *
     * Must be the transaction's FIRST read, and must not contain any plain (non-locking)
     * read: MySQL fixes a transaction's snapshot at its first plain read, so later checks
     * would not see a booking another request committed while this one waited for the lock.
     * That is why this is native SQL with a plain FOR UPDATE: JPA's PESSIMISTIC_WRITE
     * generates "FOR UPDATE OF students", which reads the joined users row without a lock,
     * fixing the snapshot before the wait (found by OfferDeskIntegrationTest).
     */
    @Query(value = "SELECT s.id FROM students s JOIN users u ON u.id = s.user_id WHERE u.email = :email FOR UPDATE",
            nativeQuery = true)
    Optional<Long> lockByUserEmail(@Param("email") String email);

    // ---- One pending offer and one placement per student, each claimed atomically.

    /** Gives the student this pending offer if they hold none and are not placed. 0 = refused. */
    @Modifying(flushAutomatically = true)
    @Query("update Student s set s.pendingOfferApplicationId = :applicationId "
            + "where s.id = :id and s.pendingOfferApplicationId is null and s.placedApplicationId is null")
    int claimPendingOffer(@Param("id") Long studentId, @Param("applicationId") Long applicationId);

    /** The pending offer was accepted: the student is now placed by it. 0 = it was not their pending offer. */
    @Modifying(flushAutomatically = true)
    @Query("update Student s set s.placedApplicationId = :applicationId, s.pendingOfferApplicationId = null "
            + "where s.id = :id and s.pendingOfferApplicationId = :applicationId and s.placedApplicationId is null")
    int placeWithPendingOffer(@Param("id") Long studentId, @Param("applicationId") Long applicationId);

    /** The pending offer was declined or expired. */
    @Modifying(flushAutomatically = true)
    @Query("update Student s set s.pendingOfferApplicationId = null "
            + "where s.id = :id and s.pendingOfferApplicationId = :applicationId")
    int releasePendingOffer(@Param("id") Long studentId, @Param("applicationId") Long applicationId);
}
