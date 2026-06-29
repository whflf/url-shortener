package com.example.core_app.repository;

import com.example.core_app.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByShortCode(String shortCode);
    Optional<Link> findByLongUrl(String longUrl);
    List<Link> findByExpiresAtBefore(LocalDateTime threshold);

    @Modifying
    @Query("DELETE FROM Link l WHERE l.expiresAt IS NOT NULL AND l.expiresAt < :threshold")
    int deleteExpiredLinks(LocalDateTime threshold);
}
