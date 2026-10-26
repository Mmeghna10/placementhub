package com.placementhub.backend.repository;

import com.placementhub.backend.entity.PlacementPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacementPolicyRepository extends JpaRepository<PlacementPolicy, Byte> {
}
