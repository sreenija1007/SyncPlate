package com.biobite.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "food_preferences")
@Data
public class FoodPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String preferenceType;

    @Column(nullable = false)
    private String preferenceValue;
}
