package com.placementhub.backend.repository;

import com.placementhub.backend.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(String status);
   Page<Job> findByStatus(String status, Pageable pageable);
    List<Job> findByRecruiterId(Long recruiterId);
    long countByStatus(String status);
}