package com.placementhub.backend.repository;

import com.placementhub.backend.entity.ApplicationTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ApplicationTimelineRepository extends JpaRepository<ApplicationTimeline, Long> {
    /** Timelines for many applications in one query (see ApplicationMapper). */
    List<ApplicationTimeline> findByApplicationIdInOrderByChangedAtAscIdAsc(Collection<Long> applicationIds);
}
