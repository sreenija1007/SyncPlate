package com.biobite.service;

import com.biobite.model.*;
import com.biobite.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final HealthConditionRepository healthConditionRepository;
    private final FoodPreferenceRepository foodPreferenceRepository;
    private final AllergyRepository allergyRepository;
    private final FoodToAvoidRepository foodToAvoidRepository;

    @Transactional
    public User registerUser(User user,
                             List<String> healthConditions,
                             List<String> foodPreferences,
                             List<String> allergies,
                             List<String> foodsToAvoid) {

        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered!");
        }
        User savedUser = userRepository.save(user);

        // Save health conditions
        if (healthConditions != null) {
            for (String condition : healthConditions) {
                HealthCondition hc = new HealthCondition();
                hc.setUser(savedUser);
                hc.setConditionName(condition);
                healthConditionRepository.save(hc);
            }
        }

        // Save food preferences
        if (foodPreferences != null) {
            for (String preference : foodPreferences) {
                FoodPreference fp = new FoodPreference();
                fp.setUser(savedUser);
                fp.setPreferenceValue(preference);
                foodPreferenceRepository.save(fp);
            }
        }

        // Save allergies
        if (allergies != null) {
            for (String allergen : allergies) {
                Allergy a = new Allergy();
                a.setUser(savedUser);
                a.setAllergen(allergen);
                allergyRepository.save(a);
            }
        }

        // Save foods to avoid
        if (foodsToAvoid != null) {
            for (String food : foodsToAvoid) {
                FoodToAvoid fta = new FoodToAvoid();
                fta.setUser(savedUser);
                fta.setFoodName(food);
                foodToAvoidRepository.save(fta);
            }
        }

        return savedUser;
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUserProfile(User user, List<String> healthConditions,
        List<String> foodPreferences, List<String> allergies, List<String> foodsToAvoid) {

        user.getHealthConditions().clear();
        user.getFoodPreferences().clear();
        user.getAllergies().clear();
        user.getFoodsToAvoid().clear();
        userRepository.save(user);

        if (healthConditions != null) {
            healthConditions.forEach(c -> {
                HealthCondition hc = new HealthCondition();
                hc.setConditionName(c);
                hc.setUser(user);
                healthConditionRepository.save(hc);
            });
        }
        if (foodPreferences != null) {
            foodPreferences.forEach(p -> {
                FoodPreference fp = new FoodPreference();
                fp.setPreferenceValue(p);
                fp.setPreferenceType("general");
                fp.setUser(user);
                foodPreferenceRepository.save(fp);
            });
        }
        if (allergies != null) {
            allergies.forEach(a -> {
                Allergy al = new Allergy();
                al.setAllergen(a);
                al.setUser(user);
                allergyRepository.save(al);
            });
        }
        if (foodsToAvoid != null) {
            foodsToAvoid.forEach(f -> {
                FoodToAvoid fta = new FoodToAvoid();
                fta.setFoodName(f);
                fta.setUser(user);
                foodToAvoidRepository.save(fta);
            });
        }
        return userRepository.save(user);
    }
}
