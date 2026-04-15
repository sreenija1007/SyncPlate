package com.biobite.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "analyses")
@Data
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String mealName;

    @Column(columnDefinition = "TEXT")
    private String inputText;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    private String overallVerdict;
    private Integer overallScore;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}