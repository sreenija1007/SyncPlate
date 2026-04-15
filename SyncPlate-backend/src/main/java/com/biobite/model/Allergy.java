package com.biobite.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "allergies")
@Data
public class Allergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String allergen;
}
