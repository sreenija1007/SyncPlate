package com.biobite.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "foods_to_avoid")
@Data
public class FoodToAvoid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String foodName;
}
