package com.placementhub.backend.enums;

/**
 * A job's placement tier, set by the placement cell when it approves the job. A placed
 * student may only take offers from a higher tier (see PlacementRules). The rank is what
 * the database stores on the student (students.placed_tier).
 */
public enum JobTier {
    REGULAR(1, "Regular"),
    DREAM(2, "Dream"),
    SUPER_DREAM(3, "Super-dream");

    private final int rank;
    private final String label;

    JobTier(int rank, String label) {
        this.rank = rank;
        this.label = label;
    }

    public int rank() {
        return rank;
    }

    public String label() {
        return label;
    }

    public static String labelOfRank(int rank) {
        for (JobTier tier : values()) {
            if (tier.rank == rank) {
                return tier.label;
            }
        }
        return "Not placed";
    }
}
