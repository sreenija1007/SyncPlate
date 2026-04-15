package com.biobite.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_reports")
@Data
public class HealthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private String reportName;       // e.g. "Blood Test - Jan 2025"
    private String reportType;       // e.g. "Blood Test", "Thyroid Panel", "Lipid Profile"

    @Column(columnDefinition = "TEXT")
    private String extractedValues;  // JSON string of key-value pairs

    @Column(columnDefinition = "TEXT")
    private String aiSummary;        // AI-generated summary

    @Column(columnDefinition = "TEXT")
    private String rawText;          // original text/OCR from report

    private LocalDateTime uploadedAt = LocalDateTime.now();
}