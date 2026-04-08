package com.example.analytics.repository;

import com.example.analytics.model.ClickDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkStatsRepository extends JpaRepository<ClickDetails, Long> {
    Optional<ClickDetails> findByCode(String code);
}
