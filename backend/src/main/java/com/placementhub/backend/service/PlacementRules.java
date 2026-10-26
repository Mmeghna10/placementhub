package com.placementhub.backend.service;

import com.placementhub.backend.dto.PlacementPolicyDto;
import com.placementhub.backend.entity.Job;
import com.placementhub.backend.entity.PlacementPolicy;
import com.placementhub.backend.entity.Student;
import com.placementhub.backend.enums.JobTier;
import com.placementhub.backend.repository.PlacementPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Offer Desk phase 3: the campus placement rules.
 *
 * A student who is not placed may take any job. A placed student may only take a job of a
 * higher tier than their current placement, and only while they have accepted fewer than
 * the maximum number of offers. Without upgrades, the maximum is one: "one student, one job".
 *
 * The same rule is enforced in three places: here (friendly messages when applying), in the
 * job search ("Recommended" hides jobs the student can no longer take), and atomically in
 * the database when an offer is made or accepted (StudentRepository.claimPendingOffer and
 * placeWithPendingOffer compare placed_tier and accepted_count in the UPDATE itself).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlacementRules {

    /** No job has a tier rank this high: "no further jobs allowed". */
    static final int NO_TIER = JobTier.SUPER_DREAM.rank() + 1;

    private final PlacementPolicyRepository policyRepository;

    public PlacementPolicy policy() {
        return policyRepository.findById(PlacementPolicy.SINGLE_ROW_ID).orElseGet(PlacementPolicy::new);
    }

    /** Most offers a student may accept, given the current rules. */
    public int maxAcceptedOffers() {
        PlacementPolicy policy = policy();
        return Boolean.TRUE.equals(policy.getAllowUpgrades()) ? policy.getMaxAcceptedOffers() : 1;
    }

    /** Lowest tier rank this student may still take (NO_TIER when none). */
    public int minimumTierRankFor(Student student) {
        if (student.getPlacedTier() == null || student.getPlacedTier() == 0) {
            return JobTier.REGULAR.rank();
        }
        if (student.getAcceptedCount() >= maxAcceptedOffers()) {
            return NO_TIER;
        }
        return student.getPlacedTier() + 1;
    }

    /** Why this student may not take this job, if the rules forbid it. */
    public Optional<String> refusalReason(Student student, Job job) {
        int minimum = minimumTierRankFor(student);
        if (job.getTier().rank() >= minimum) {
            return Optional.empty();
        }
        String placed = JobTier.labelOfRank(student.getPlacedTier());
        if (minimum == NO_TIER) {
            return Optional.of("You are placed (" + placed + ") and have accepted the maximum number of offers the "
                    + "placement cell allows, so you can't apply to more jobs.");
        }
        return Optional.of("You are placed (" + placed + "), so only jobs of a higher tier are open to you. This job is "
                + job.getTier().label() + ".");
    }

    @Transactional
    public PlacementPolicy update(PlacementPolicyDto request) {
        PlacementPolicy policy = policy();
        policy.setAllowUpgrades(request.getAllowUpgrades());
        policy.setMaxAcceptedOffers(request.getMaxAcceptedOffers());
        return policyRepository.saveAndFlush(policy);
    }
}
