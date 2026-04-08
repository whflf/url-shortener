package com.example.analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String browser;
    private String os;
    private String deviceType;
    private String referer;
    private LocalDateTime timestamp;
}
