package com.placementhub.backend.repository;

import com.placementhub.backend.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStudentId(Long studentId);
    List<Application> findByJobId(Long jobId);
    boolean existsByStudentIdAndJobId(Long studentId, Long jobId);
    long countByStatus(String status);
}