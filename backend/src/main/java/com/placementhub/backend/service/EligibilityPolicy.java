package com.placementhub.backend.service;

import com.placementhub.backend.entity.Job;
import com.placementhub.backend.entity.Student;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Whether a student meets a job's requirements. Used both to filter "Recommended" jobs and
 * to accept or refuse an application, so the two can never disagree.
 *
 * A job with a requirement needs the matching profile field: a student with no CGPA (or no
 * branch) is not eligible for a job that sets a minimum CGPA (or limits branches).
 * Branch matching is a case-insensitive "contains" on the job's free-text list, the same
 * as before; replacing free text with a fixed branch list is a known future improvement.
 */
@Component
public class EligibilityPolicy {

    /** @return why the student is not eligible, or empty if they are. */
    public Optional<String> ineligibilityReason(Job job, Student student) {
        if (job.getMinCgpa() != null) {
            if (student.getCgpa() == null) {
                return Optional.of("This job requires a minimum CGPA of " + job.getMinCgpa().toPlainString()
                        + ". Add your CGPA to your profile first.");
            }
            if (student.getCgpa().compareTo(job.getMinCgpa()) < 0) {
                return Optional.of("This job requires a minimum CGPA of " + job.getMinCgpa().toPlainString() + ".");
            }
        }

        String branches = job.getEligibleBranches();
        if (branches != null && !branches.isBlank()) {
            if (student.getBranch() == null || student.getBranch().isBlank()) {
                return Optional.of("This job is open to: " + branches.trim()
                        + ". Add your branch to your profile first.");
            }
            if (!branches.toLowerCase().contains(student.getBranch().trim().toLowerCase())) {
                return Optional.of("This job is only open to: " + branches.trim() + ".");
            }
        }

        return Optional.empty();
    }

    public boolean isEligible(Job job, Student student) {
        return ineligibilityReason(job, student).isEmpty();
    }
}
