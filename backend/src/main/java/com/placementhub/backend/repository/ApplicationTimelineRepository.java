package com.placementhub.backend.repository;

import com.placementhub.backend.entity.ApplicationTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationTimelineRepository extends JpaRepository<ApplicationTimeline, Long> {
    List<ApplicationTimeline> findByApplicationIdOrderByChangedAtAsc(Long applicationId);
}