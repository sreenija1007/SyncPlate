package com.biobite.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Integer age;
    private String city;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String dietaryPreference;
    private String healthGoal;
    private String cookingStyle;
    private String cuisinePreference;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<HealthCondition> healthConditions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<FoodPreference> foodPreferences;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Allergy> allergies;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<FoodToAvoid> foodsToAvoid;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "profile_completed")
    private Boolean profileCompleted = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "date_of_birth")
    private String dateOfBirth;
}