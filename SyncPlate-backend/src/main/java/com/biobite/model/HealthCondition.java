package com.biobite.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "health_conditions")
@Data
public class HealthCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String conditionName;
}
