package com.placementhub.backend.repository;

import com.placementhub.backend.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Every job listing maps the company name, so list queries fetch the company in the same
// SQL statement (to-one join, so it is also safe with pagination).
public interface JobRepository extends JpaRepository<Job, Long> {

    @EntityGraph(attributePaths = "company")
    List<Job> findByStatus(String status);

    @EntityGraph(attributePaths = "company")
    Page<Job> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = "company")
    List<Job> findByRecruiterId(Long recruiterId);

    @Override
    @EntityGraph(attributePaths = "company")
    Page<Job> findAll(Pageable pageable);

    long countByStatus(String status);
}
