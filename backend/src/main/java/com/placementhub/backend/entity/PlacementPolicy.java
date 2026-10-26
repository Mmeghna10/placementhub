package com.placementhub.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** The campus placement rules: a single row (id = 1) edited by the placement cell. */
@Entity
@Table(name = "placement_policy")
@Data
public class PlacementPolicy {

    public static final byte SINGLE_ROW_ID = 1;

    @Id
    private Byte id = SINGLE_ROW_ID;

    /** May a placed student take an offer from a higher tier? */
    @Column(name = "allow_upgrades", nullable = false)
    private Boolean allowUpgrades = true;

    /** How many offers a student may accept in total (counting released ones). */
    @Column(name = "max_accepted_offers", nullable = false)
    private Integer maxAcceptedOffers = 2;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
