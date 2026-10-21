package com.placementhub.backend.repository;

import com.placementhub.backend.entity.Recruiter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {

    // Responses include the user's email and the company details.
    @EntityGraph(attributePaths = {"user", "company"})
    Optional<Recruiter> findByUser_Email(String email);

    @Override
    @EntityGraph(attributePaths = {"user", "company"})
    Page<Recruiter> findAll(Pageable pageable);
}
