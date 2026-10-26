package com.placementhub.backend.service;

import com.placementhub.backend.entity.Job;
import com.placementhub.backend.entity.Student;
import com.placementhub.backend.enums.ApplicationStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.placementhub.backend.enums.ApplicationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Pure rule tests (no Spring, no database) for the Milestone 4 state machine and eligibility. */
class Milestone4RulesTest {

    @Nested
    class ApplicationStateMachine {

        @ParameterizedTest(name = "{0} -> {1} allowed={2}")
        @CsvSource({
                "APPLIED,     SHORTLISTED, true",
                "APPLIED,     REJECTED,    true",
                "APPLIED,     INTERVIEW,   false",
                "APPLIED,     OFFERED,     false",
                "SHORTLISTED, INTERVIEW,   true",
                "SHORTLISTED, REJECTED,    true",
                "SHORTLISTED, APPLIED,     false",
                "INTERVIEW,   OFFERED,     true",
                "INTERVIEW,   REJECTED,    true",
                "INTERVIEW,   SHORTLISTED, false",
                "OFFERED,     REJECTED,    false",
                "OFFERED,     ACCEPTED,    false",
                "OFFERED,     DECLINED,    false",
                "ACCEPTED,    DECLINED,    false",
                "EXPIRED,     OFFERED,     false",
                "INTERVIEW,   WAITLISTED,  true",
                "WAITLISTED,  OFFERED,     true",
                "WAITLISTED,  REJECTED,    true",
                "WAITLISTED,  INTERVIEW,   false",
                "SHORTLISTED, WAITLISTED,  false",
                "REJECTED,    OFFERED,     false",
                "REJECTED,    APPLIED,     false",
                "APPLIED,     APPLIED,     false",
        })
        void transitions(ApplicationStatus from, ApplicationStatus to, boolean allowed) {
            assertThat(from.canMoveTo(to)).isEqualTo(allowed);
        }

        @Test
        void decidedOffersAndRejectionsAreFinal() {
            // An OFFERED application is waiting for the student: not final, but the
            // recruiter has no moves left (accept/decline belong to the student).
            assertThat(OFFERED.isFinal()).isFalse();
            assertThat(OFFERED.allowedNext()).isEmpty();
            assertThat(ACCEPTED.isFinal()).isTrue();
            assertThat(DECLINED.isFinal()).isTrue();
            assertThat(EXPIRED.isFinal()).isTrue();
            assertThat(REJECTED.isFinal()).isTrue();
            assertThat(APPLIED.isFinal()).isFalse();
        }
    }

    @Nested
    class Eligibility {

        private final EligibilityPolicy policy = new EligibilityPolicy();

        private Job job(String minCgpa, String branches) {
            Job job = new Job();
            job.setMinCgpa(minCgpa == null ? null : new BigDecimal(minCgpa));
            job.setEligibleBranches(branches);
            return job;
        }

        private Student student(String cgpa, String branch) {
            Student student = new Student();
            student.setCgpa(cgpa == null ? null : new BigDecimal(cgpa));
            student.setBranch(branch);
            return student;
        }

        @Test
        void noRequirementsMeansEveryoneIsEligible() {
            assertThat(policy.isEligible(job(null, null), student(null, null))).isTrue();
            assertThat(policy.isEligible(job(null, "  "), student(null, null))).isTrue();
        }

        @Test
        void cgpaMustMeetTheMinimum() {
            assertThat(policy.isEligible(job("7.50", null), student("7.50", null))).isTrue();
            assertThat(policy.ineligibilityReason(job("7.50", null), student("7.49", null)))
                    .hasValueSatisfying(reason -> assertThat(reason).contains("minimum CGPA of 7.50"));
        }

        @Test
        void missingProfileFieldsAreNotTreatedAsMatching() {
            // Before Milestone 4 a student without CGPA/branch counted as eligible for everything.
            assertThat(policy.ineligibilityReason(job("6.00", null), student(null, "CSE")))
                    .hasValueSatisfying(reason -> assertThat(reason).contains("Add your CGPA"));
            assertThat(policy.ineligibilityReason(job(null, "CSE, IT"), student("9.00", null)))
                    .hasValueSatisfying(reason -> assertThat(reason).contains("Add your branch"));
        }

        @Test
        void branchMatchIsCaseInsensitive() {
            assertThat(policy.isEligible(job(null, "CSE, IT, ECE"), student(null, "it"))).isTrue();
            assertThat(policy.isEligible(job(null, "CSE, IT"), student(null, "Mechanical"))).isFalse();
        }
    }
}
