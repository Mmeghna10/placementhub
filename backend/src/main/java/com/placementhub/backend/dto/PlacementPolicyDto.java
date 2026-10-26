package com.placementhub.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** The campus placement rules, as read and edited by the placement cell. */
@Data
public class PlacementPolicyDto {

    /** May a placed student take an offer from a higher tier? */
    @NotNull(message = "allowUpgrades is required")
    private Boolean allowUpgrades;

    /** Offers a student may accept in total, including released ones (1-5). */
    @NotNull(message = "maxAcceptedOffers is required")
    @Min(value = 1, message = "A student must be able to accept at least 1 offer")
    @Max(value = 5, message = "At most 5 accepted offers")
    private Integer maxAcceptedOffers;
}
