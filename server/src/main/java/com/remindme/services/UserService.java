package com.remindme.services;

import com.remindme.models.User;
import com.remindme.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<String, Object> getUserById(String userId) {
        try {
            User user = userRepository.getUser(userId);
            if (user != null) {
                return user.getSimplifiedView();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error retrieving user data");
        }
    }
}
