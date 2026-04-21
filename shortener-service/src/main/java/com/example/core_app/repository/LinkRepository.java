package com.example.core_app.repository;

import com.example.core_app.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByShortCode(String shortCode);
    Optional<Link> findByLongUrl(String longUrl);
}
